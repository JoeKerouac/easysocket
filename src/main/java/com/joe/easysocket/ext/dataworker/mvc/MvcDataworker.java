package com.joe.easysocket.ext.dataworker.mvc;

import com.joe.concurrent.ThreadUtil;
import com.joe.easysocket.common.DatagramUtil;
import com.joe.easysocket.common.Function;
import com.joe.easysocket.data.Datagram;
import com.joe.easysocket.data.ProtocolData;
import com.joe.easysocket.exception.NoRequireParamException;
import com.joe.easysocket.ext.CustomDeque;
import com.joe.easysocket.ext.DataWorker;
import com.joe.easysocket.ext.PublishCenter;
import com.joe.easysocket.ext.dataworker.mvc.coder.DataReaderContainer;
import com.joe.easysocket.ext.dataworker.mvc.coder.DataWriterContainer;
import com.joe.easysocket.ext.dataworker.mvc.coder.ReaderInterceptor;
import com.joe.easysocket.ext.dataworker.mvc.coder.WriterInterceptor;
import com.joe.easysocket.ext.dataworker.mvc.container.BeanContainerImpl;
import com.joe.easysocket.ext.dataworker.mvc.context.RequestContext;
import com.joe.easysocket.ext.dataworker.mvc.context.ResponseContext;
import com.joe.easysocket.ext.dataworker.mvc.context.session.Session;
import com.joe.easysocket.ext.dataworker.mvc.context.session.SessionManager;
import com.joe.easysocket.ext.dataworker.mvc.context.session.SessionManagerImpl;
import com.joe.easysocket.ext.dataworker.mvc.data.BaseDTO;
import com.joe.easysocket.ext.dataworker.mvc.data.InterfaceData;
import com.joe.easysocket.ext.dataworker.mvc.exception.FilterException;
import com.joe.easysocket.ext.dataworker.mvc.exception.MediaTypeNoSupportException;
import com.joe.easysocket.ext.dataworker.mvc.exception.ParamValidationException;
import com.joe.easysocket.ext.dataworker.mvc.exception.ResourceNotFoundException;
import com.joe.easysocket.ext.dataworker.mvc.exceptionmapper.ExceptionMapper;
import com.joe.easysocket.ext.dataworker.mvc.exceptionmapper.ExceptionMapperContainer;
import com.joe.easysocket.ext.dataworker.mvc.filter.FilterContainer;
import com.joe.easysocket.ext.dataworker.mvc.param.ParamParserContainer;
import com.joe.easysocket.ext.dataworker.mvc.resource.Resource;
import com.joe.easysocket.ext.dataworker.mvc.resource.ResourceContainer;
import com.joe.parse.json.JsonParser;
import com.joe.utils.StringUtils;
import com.joe.utils.Tools;
import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 数据处理器，处理协议栈传过来的数据，并且给出结果（目前异常处理的时候没有处理异常处理器异常的情况）
 * <p>
 * 该实现中register为系统调用，手动调用无效
 * <p>
 * 其中BeanContainer可以提供最基础的实现，但是建议自己实现并注册，默认容器仅用于测试
 *
 * @author joe
 */
public class MvcDataworker implements DataWorker {
    private static final Logger logger = LoggerFactory.getLogger(MvcDataworker.class);
    private static final JsonParser parser = JsonParser.getInstance();
    private ResourceContainer resourceContainer;
    private FilterContainer filterContainer;
    private DataWriterContainer dataWriterContainer;
    private DataReaderContainer dataReaderContainer;
    private ExceptionMapperContainer exceptionMapperContainer;
    private SessionManager sessionManager;
    private BeanContainer beanContainer;
    private ParamParserContainer paramParserContainer;
    //发布中心
    private PublishCenter publishCenter;
    //发布中心监听的消息通道
    private String channel;
    //双向队列
    private CustomDeque<ProtocolData> deque;
    //执行任务的线程池
    private ExecutorService service;
    //关闭标志，该标志位主要为了尽快获取写锁（读锁可以重入，如果读锁一直重入写锁就可能无法获取，也就是该数据处理
    // 器就有可能无法关闭），详情参考关闭逻辑和运行时数据处理逻辑
    private AtomicBoolean shutdown = new AtomicBoolean(true);
    //关闭锁，关闭时需要加锁，必须是读写锁，关闭的时候加写锁，数据处理器工作时加读锁
    private ReentrantReadWriteLock shutdownLock = new ReentrantReadWriteLock();
    //关闭回调，系统关闭时会调用（包括使用kill信号）
    private Function callback;

    public MvcDataworker(MvcDataworkerConfig mvcDataworkerConfig) {
        register(mvcDataworkerConfig.beanContainer);
        register(mvcDataworkerConfig.deque);
        register(mvcDataworkerConfig.publishCenter, mvcDataworkerConfig.channel);
        register(mvcDataworkerConfig.sessionManager);
    }

    @Builder
    public static class MvcDataworkerConfig {
        private CustomDeque<ProtocolData> deque;
        private PublishCenter publishCenter;
        //发布中心往底层发布的channel
        private String channel;
        private SessionManager sessionManager;
        private BeanContainer beanContainer;
    }

    @Override
    public void register(SessionManager sessionManager) {
        this.sessionManager = sessionManager == null ? new SessionManagerImpl() : sessionManager;
    }

    @Override
    public void register(BeanContainer beanContainer) {
        this.beanContainer = beanContainer == null ? new BeanContainerImpl("com") : beanContainer;
    }

    @Override
    public synchronized String start(String name, Function callback) {
        if (!shutdown.get()) {
            logger.debug("服务器已经启动，请勿重复启动");
            return name;
        }
        shutdown.set(false);
        init();
        this.callback = callback;
        name = StringUtils.isEmpty(name) ? Tools.createNonceStr(4) : name;
        new Thread(this, "数据处理器工作线程-" + name).start();
        //添加关闭监听
        addCloseListener();
        return name;
    }

    @Override
    public synchronized void shutdown() {
//      销毁数据处理器，销毁逻辑：首先将标志位变为true，然后尝试加锁，加锁成功后再进行销毁，从队列中读取消息后也需要加锁，
//      如果加锁失败则中断读取，防止从队列中读取到消息后被强制关闭而造成消息丢失，标志位则是防止销毁线程一直获取不到锁而
//      添加的。

        if (shutdown.get()) {
            logger.debug("服务器已经关闭，请勿重复关闭");
            return;
        }

        shutdown.set(true);
        try {
//            加锁，此处需要加写锁，加该锁后读锁就不能重入了，防止关闭时锁被重入（主要是为了数据处理加锁时的效率
//            考虑，数据处理时锁可以重入，关闭时就不能重入了）
            shutdownLock.writeLock().lock();
            logger.info("销毁MVC数据处理器");

            beanContainer.destroy();
            sessionManager.destroy();

            resourceContainer.destroy();
            filterContainer.destroy();
            dataWriterContainer.destroy();
            dataReaderContainer.destroy();
            exceptionMapperContainer.destroy();
            paramParserContainer.destroy();
            service.shutdown();
            logger.info("MVC数据处理器销毁成功");
        } finally {
            shutdownLock.writeLock().unlock();
        }

        if (callback != null) {
            callback.exec();
        }
    }

    @Override
    public void register(CustomDeque<ProtocolData> deque) {
        this.deque = deque;
    }

    @Override
    public void register(PublishCenter publishCenter, String channel) {
        this.publishCenter = publishCenter;
        this.channel = StringUtils.isEmpty(channel) ? "/protocol/receive" : channel;
    }

    /**
     * run方法会自动判断当前服务器状态
     */
    @Override
    public void run() {
        logger.debug("MVC数据处理器开始工作");
        while (true) {
            if (shutdown.get()) {
                logger.debug("服务器已经关闭");
                return;
            }

            try {
                //使用读锁，防止效率降低（读锁可以重入）
                shutdownLock.readLock().lock();
                read();
            } finally {
                shutdownLock.readLock().unlock();
            }
        }
    }

    private void init() {
        logger.info("开始初始化MVC数据处理器");
        //检查是否有队列和发布中心
        if (this.publishCenter == null || this.deque == null) {
            logger.error("协议栈缺少队列或者发布中心，请先注册队列或者发布中心");
            throw new NoRequireParamException(this.publishCenter == null ? "PublishCenter" : null, this.deque == null
                    ? "Deque" : null);
        }

        //判断session管理器是否为null，为null则使用默认
        this.sessionManager = sessionManager == null ? new SessionManagerImpl() : this.sessionManager;
        //判断bean容器是否为null，不为null则使用默认
        this.beanContainer = beanContainer == null ? new BeanContainerImpl("com") : this.beanContainer;

        service = ThreadUtil.createPool(ThreadUtil.PoolType.IO);

        beanContainer.init();
        sessionManager.init();
        resourceContainer = new ResourceContainer(beanContainer);
        filterContainer = new FilterContainer(beanContainer);
        dataWriterContainer = new DataWriterContainer(beanContainer);
        paramParserContainer = new ParamParserContainer(beanContainer);
        dataReaderContainer = new DataReaderContainer(beanContainer, paramParserContainer);
        exceptionMapperContainer = new ExceptionMapperContainer(beanContainer);

        resourceContainer.init();
        filterContainer.init();
        dataWriterContainer.init();
        dataReaderContainer.init();
        exceptionMapperContainer.init();
        paramParserContainer.init();
        logger.info("MVC数据处理器初始化完毕");
    }

    private void read() {
        ProtocolData protocolData = null;
        try {
            logger.debug("开始从队列读取数据");
            //隔200毫秒检查一次服务器状态，防止因为队列中一直没有数据或者一直没有读取到数据造成服务器无法关闭
            while (protocolData == null) {
                protocolData = this.deque.pollFirst(200, TimeUnit.MILLISECONDS);

                if (shutdown.get()) {
                    logger.debug("服务器已经关闭");
                    break;
                }
            }

            //只有在服务器关闭的情况下才有可能等于null
            if (protocolData == null) {
                logger.debug("服务器已经关闭");
                return;
            }
        } catch (Throwable e) {
            logger.error("数据读取异常", e);
        }

        try {
            logger.debug("从队列中读取到数据：{}", protocolData);
            byte[] data = protocolData.getData();
            ProtocolData.ChannelInfo channelInfo = protocolData.getChannelInfo();

            if (data != null || channelInfo == null) {
                Datagram datagram = DatagramUtil.decode(data);
                if (worker(datagram.getType())) {
                    logger.debug("该数据可以处理，提交到线程池开始处理");
                    service.submit(() -> {
                        ProtocolData result = accept(datagram, channelInfo);
                        logger.debug("MVC数据处理器处理{}的结果为：{}；将该结果发送至底层，对应的通道信息为：{}", datagram, result, channelInfo);
                        publishCenter.pub(channel, result);
                    });
                }
            } else {
                logger.debug("数据异常，异常数据为：{}", protocolData);
            }
        } catch (Throwable e) {
            logger.error("数据处理中发生异常，数据为：{}", protocolData, e);
        }
    }

    private ProtocolData accept(Datagram datagram, ProtocolData.ChannelInfo channelInfo) {
        logger.debug("接收到数据，开始处理。{}，对应的channelinfo为：{}", datagram, channelInfo);
        RequestContext requestContext = null;
        ResponseContext responseContext = null;
        InterfaceData resultData;
        InterfaceData message = null;
        try {
            Datagram requestDatagram = datagram;
            byte[] body = requestDatagram.getBody();

            if (body == null || body.length == 0) {
                //请求必须有请求体，否则最基本的invoke信息都没有
                logger.warn("该请求没有请求体，请求内容：{}", datagram);
                return null;
            }

            //必须在此处初始化，否则当发生异常的时候异常中获取到的requestContext是空，无法获取到信息
            requestContext = new RequestContext(channelInfo.getChannel() , requestDatagram , requestDatagram.getCharset());
            // MVC数据处理器只有这一种请求data，直接读取
            logger.debug("开始解析请求数据");
            message = parser.readAsObject(body, InterfaceData.class);
            logger.debug("请求数据解析完毕，请求数据为：{}", message);

            logger.debug("开始构建请求上下文");
            // 构建请求上下文
            Session session = sessionManager.get(channelInfo);
            requestContext.setSession(session);
            logger.debug("请求上下文构建成功，开始查找请求的资源");


            // 搜索指定的resource
            Resource resource = findResource(message.getInvoke());
            // 放进请求上下文
            requestContext.setResource(resource);
            logger.debug("请求的资源查找完成，请求资源为：{}", resource);

            // 开始查找数据编码器
            logger.debug("开始查找数据编码器");
            // 找到请求数据编码处理器
            ReaderInterceptor readerInterceptor = findReaderInterceptor(resource.getConsume());
            requestContext.setReader(readerInterceptor);
            logger.debug("数据编码器为：{}", readerInterceptor);

            logger.debug("开始解码参数");
            // 开始解码数据
            Object[] param = readerInterceptor.read(resource.getParams(), requestContext, message.getData());
            requestContext.setParams(param);
            logger.debug("参数解码完毕，参数为：{}", param);

            logger.debug("开始验证参数");
            resource.check(param);
            logger.debug("参数验证完毕");

            logger.debug("开始请求filter");
            // 请求filter
            filterContainer.requestFilter(requestContext.getRequest());
            logger.debug("filter完毕，开始调用资源");

            // 调用资源
            Object result = resource.invoke(requestContext);
            // 构建响应上下文
            responseContext = new ResponseContext();
            responseContext.getResponse().setResult(result);
            logger.debug("资源调用完毕，请求结果为：{}", result);

            // 响应
            logger.debug("开始处理响应");
            resultData = response(requestContext, responseContext, message);
            logger.debug("响应处理完毕");
        } catch (ResourceNotFoundException e) {
            logger.warn("用户请求的资源不存在", e);
            resultData = buildResult(requestContext.getSource(), new BaseDTO<>("404"), message.getId(),
                    message.getInvoke(), findWriterInterceptor(null));
        } catch (MediaTypeNoSupportException e) {
            logger.warn("找不到对应的参数解析器", e);
            resultData = buildResult(requestContext.getSource(), new BaseDTO<>("401"), message.getId(),
                    message.getInvoke(), resolveDataInterceptor(requestContext, responseContext));
        } catch (ParamValidationException e) {
            logger.warn("参数验证失败");
            resultData = buildResult(requestContext.getSource(), BaseDTO.buildError("400"),
                    message.getId(), message.getInvoke(), resolveDataInterceptor(requestContext, responseContext));
        } catch (Throwable e) {
            // 请求过程中发生了异常
            logger.debug("请求过程中发生了异常，开始查找相应的异常处理器处理异常");

            // 查找异常处理器
            List<ExceptionMapper> exceptionMappers = exceptionMapperContainer.select(mapper -> {
                return mapper.mapper(e);
            });

            logger.debug("异常处理器查找完毕");
            if (exceptionMappers.isEmpty()) {
                logger.warn("异常没有找到相应的处理器", e);
                throw e;
            } else {
                logger.debug("找到异常处理器，由相应的异常处理器处理");
                ResponseContext.Response response = exceptionMappers.get(0).toResponse(e);
                resultData = new InterfaceData(message.getId(), message.getInvoke(),
                        resolveDataInterceptor(requestContext, responseContext).write(response.getResult()));
            }
        }

        return new ProtocolData(DatagramUtil.build(parser.toJson(resultData).getBytes(), (byte) 1, (byte) 1)
                .getData(), new ProtocolData.ChannelInfo(channelInfo.getChannel()));
    }

    /**
     * 响应处理
     *
     * @param requestContext  请求上下文
     * @param responseContext 响应上下文
     * @param userData        用户发来的请求数据
     * @return 响应数据
     * @throws MediaTypeNoSupportException 找不到相应数据的编码器
     * @throws FilterException             filter异常
     */
    private InterfaceData response(RequestContext requestContext, ResponseContext responseContext, InterfaceData
            userData)
            throws MediaTypeNoSupportException, FilterException {
        logger.debug("开始构建响应");
        RequestContext.RequestWrapper request = requestContext.getRequest();
        Resource resource = requestContext.getResource();
        ResponseContext.Response response = responseContext.getResponse();
        //该消息的来源
        String src = requestContext.getSource();

        // 请求过程中没有发生异常
        // 响应filter
        filterContainer.responseFilter(request, response);

        // 找到处理响应的编码器
        WriterInterceptor writerInterceptor = findWriterInterceptor(resource.getProduce());
        responseContext.setWriter(writerInterceptor);

        Object result = response.getResult();

        logger.debug("请求结果为：{}", result);

        // 根据不同的结果分别处理，资源有可能没有返回，有可能返回聊天消息，也有可能返回正常的数据
        if (result == null) {
            logger.debug("请求的接口{}没有响应消息，返回一个BaseDTO", userData.getInvoke());
            // 如果该接口没有响应消息，那么返回一个基本的请求成功
            InterfaceData data = buildResult(src, BaseDTO.buildSuccess(), userData.getId(),
                    userData.getInvoke(), writerInterceptor);
            return data;
        } else if (result instanceof InterfaceData) {
            logger.debug("用户响应的信息是InterfaceData对象");
            InterfaceData data = (InterfaceData) result;
            return data;
        } else {
            logger.debug("请求接口{}的响应信息为：{}", userData.getInvoke(), result);
            // 该接口有响应消息并且不是聊天类型消息，那么直接将该消息返回
            InterfaceData data = buildResult(src, result, userData.getId(), userData.getInvoke(),
                    writerInterceptor);
            return data;
        }
    }

    /**
     * 构建响应结果
     *
     * @param srcOrDest         消息来源或者目的地
     * @param result            响应的结果
     * @param id                对应的请求消息的ID
     * @param invoke            对应的请求消息请求的接口名
     * @param writerInterceptor 对应的数据处理器
     * @return 响应结果
     */
    private InterfaceData buildResult(String srcOrDest, Object result, String id, String invoke,
                                      WriterInterceptor writerInterceptor) {
        invoke = invoke.startsWith("/") ? "/back" + invoke : "/back/" + invoke;
        if (writerInterceptor == null) {
            return null;
        }
        return new InterfaceData(id, invoke, writerInterceptor.write(result));
    }

    /**
     * 查找数据解码器
     *
     * @param consume 待读取内容的类型
     * @return 指定类型对应的数据解码器
     * @throws MediaTypeNoSupportException 找不到指定类型数据解码器时抛出该异常
     */
    private ReaderInterceptor findReaderInterceptor(String consume) throws MediaTypeNoSupportException {
        logger.debug("要查找的数据解码器类型为：{}", consume);
        final String realConsume = StringUtils.isEmpty(consume) ? "json" : consume;
        List<ReaderInterceptor> readerInterceptors = dataReaderContainer.select(reader -> {
            return reader.isReadable(realConsume);
        });

        logger.debug("数据编码器为：{}", readerInterceptors);
        if (readerInterceptors.isEmpty()) {
            // 找不到支持
            throw new MediaTypeNoSupportException(consume);
        }

        return readerInterceptors.get(0);
    }

    /**
     * 查找resource
     *
     * @param path resource的名字
     * @return 要查找的resource
     * @throws ResourceNotFoundException 找不到指定resource时抛出该异常
     */
    private Resource findResource(String path) throws ResourceNotFoundException {
        if (StringUtils.isEmpty(path)) {
            logger.error("请求信息中没有请求资源的名字");
            throw new ResourceNotFoundException(String.valueOf(path));
        }
        Resource resource = resourceContainer.findResource(path);
        logger.debug("请求的资源为：{}", resource);

        if (resource == null) {
            // 找不到要请求的resource
            logger.error("没有找到{}对应的资源，请检查请求地址是否有误", path);
            throw new ResourceNotFoundException(path);
        }
        return resource;
    }

    /**
     * 查找数据编码器
     *
     * @param produce 要处理的数据的类型
     * @return 指定类型对应的数据编码器
     * @throws MediaTypeNoSupportException 找不到指定编码器时抛出该异常
     */
    private WriterInterceptor findWriterInterceptor(String produce) throws MediaTypeNoSupportException {
        logger.debug("查找{}格式的数据编码器", produce);
        final String dataProduce = StringUtils.isEmpty(produce) ? "json" : produce;

        List<WriterInterceptor> writerInterceptors = dataWriterContainer.select(dataInterceptor -> {
            return dataInterceptor.isWriteable(dataProduce);
        });

        if (writerInterceptors.isEmpty()) {
            // 找不到支持
            throw new MediaTypeNoSupportException(String.valueOf(dataProduce));
        }
        return writerInterceptors.get(0);
    }

    /**
     * 确定一个响应数据处理器
     *
     * @param requestContext  请求上下文
     * @param responseContext 响应上下文
     * @return 响应数据处理器，该方法肯定会返回一个响应数据处理器
     */
    private WriterInterceptor resolveDataInterceptor(RequestContext requestContext, ResponseContext responseContext) {
        logger.debug("根据上下文确定一个响应数据处理器");
        WriterInterceptor writer = responseContext.getWriter();
        if (writer != null) {
            logger.debug("响应过程中已经确定了响应数据处理器，直接返回");
            return writer;
        } else {
            Resource resource = requestContext.getResource();
            try {
                return findWriterInterceptor(resource.getProduce());
            } catch (Exception e) {
                return findWriterInterceptor(null);
            }
        }
    }

    private boolean worker(int type) {
        return type == 1;
    }

    /**
     * 添加关闭监听，该监听可以监听kill PID，也可以监听System.exit()，但是对kill -9 PID无效
     */
    private void addCloseListener() {
        // 该关闭监听针对kill PID
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.warn("系统监听到关闭信号，即将关闭");
            shutdown();
        }));
    }
}
