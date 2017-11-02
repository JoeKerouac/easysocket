package com.joe.easysocket.server.protocol;

import com.joe.easysocket.server.data.ProtocolData;
import com.joe.easysocket.server.exception.NoRequireParamException;
import com.joe.easysocket.server.ext.CustomDeque;
import com.joe.easysocket.server.ext.CustomMessageListener;
import com.joe.easysocket.server.ext.EventCenter;
import com.joe.easysocket.server.ext.PublishCenter;
import com.joe.utils.common.StringUtils;
import com.joe.utils.concurrent.ThreadUtil;
import lombok.Builder;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

/**
 * 协议栈基本实现
 *
 * @author joe
 */
public class ProtocolImpl implements Protocol {
    private static final Logger logger = LoggerFactory.getLogger(ProtocolImpl.class);
    //协议栈事件中心
    private List<EventCenter> eventCenters;
    //当前所有通道，key为链接的ID，value为通道
    private Map<String, PChannel> pChannels;
    //发布中心，用于接受应用层主动发往底层的数据
    private PublishCenter publishCenter;
    //线程池，用于处理底层数据
    private ExecutorService service;
    //当前协议栈是否初始化，默认未初始化
    private boolean init = false;
    //当前协议栈是否销毁，默认未销毁
    private volatile boolean destroy = false;
    //双向阻塞队列
    private CustomDeque<ProtocolData> deque;
    //发布中心的消息通道
    private String channel;
    //心跳周期
    private int heartbeat;
    //过期连接清理线程
    private Thread cleanupThread;

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
     * 默认构造器
     *
     * @param publishCenter 发布中心
     * @param channel       发布中心对应的通道
     * @param deque         队列
     * @param heartbeaat    最长心跳周期，单位为秒，超过该时间没有收到客户端任何消息后关闭该连接（该时间最小为30秒）
     */
    @Builder
    ProtocolImpl(@NonNull PublishCenter publishCenter, String channel, @NonNull CustomDeque<ProtocolData> deque, int
            heartbeaat) {
        register(publishCenter, channel);
        register(deque);
        this.heartbeat = heartbeaat < 30 ? 30 : heartbeaat;
    }


    @Override
    public synchronized void init() {
        logger.info("初始化协议栈，心跳周期为：{}秒", heartbeat);
        if (init) {
            logger.warn("协议栈已经初始化，不能重复初始化");
            return;
        }

        //检查是否有队列和发布中心
        if (this.publishCenter == null || this.deque == null) {
            logger.error("协议栈缺少队列或者发布中心，请先注册队列或者发布中心");
            throw new NoRequireParamException(this.publishCenter == null ? "PublishCenter" : null, this.deque == null
                    ? "Deque" : null);
        }

        //注册应用层消息监听者，监听应用层消息
        publishCenter.register(this.channel, new CustomMessageListener<ProtocolData>() {
            @Override
            public void onMessage(byte[] channel, ProtocolData message) {
                logger.debug("协议栈从发布中心获取到一个数据：{}", message);
                send(message);
            }

            @Override
            public Class<ProtocolData> resolveMessageType() {
                return ProtocolData.class;
            }
        });

        eventCenters = new CopyOnWriteArrayList<>();
        pChannels = new ConcurrentHashMap<>();

        service = ThreadUtil.createPool(ThreadUtil.PoolType.IO);

        cleanupThread = new Thread(() -> {
            while (true) {
                try {
                    long now = System.currentTimeMillis();
                    pChannels.forEach((id, channel) -> {
                        long last = channel.getLastActive();
                        long circle = (now - last) / 1000;
                        if ((circle - heartbeat) > 0) {
                            logger.debug("连接{}心跳超时", id);
                            //心跳超时，关闭连接
                            publish(id, ProtocolEvent.UNREGISTER, id, CloseCause.TIMEOUT);
                            channel.close();
                            pChannels.remove(id);
                        }
                    });
                } catch (Throwable e) {
                    logger.error("过期连接清理线程出错", e);
                }
                //每隔1/5的心跳周期检测一次，也就是过期连接最长会延长1/5的心跳周期被清理
                try {
                    Thread.sleep(heartbeat / 5 * 1000);
                } catch (InterruptedException e) {
                    logger.debug("应用关闭，清理线程被中断");
                    break;
                }
            }
        }, "过期连接清理线程");
        cleanupThread.start();

        init = true;
        logger.info("协议栈初始化完成");
    }

    @Override
    public synchronized void destroy() {
        logger.info("开始销毁协议栈");
        if (!init || destroy) {
            logger.warn("协议栈已经销毁或者还未初始化，无法销毁");
            return;
        }

        eventCenters.clear();
        pChannels.clear();
        service.shutdown();
        cleanupThread.interrupt();
        destroy = true;
        logger.info("协议栈销毁成功");
    }

    @Override
    public void register(@NonNull PChannel channel) {
        logger.debug("注册ID为：{}的连接{}", channel.id(), channel);
        String id = channel.id();

        if (this.pChannels.containsKey(id) && channel != this.pChannels.get(id)) {
            logger.warn("当前连接池中存在id为{}的通道，并且该通道与新通道不是同一个通道，将注销该通道并注册新的通道");
            close(id, CloseCause.SYSTEM);
            this.pChannels.put(id, channel);
        } else if (this.pChannels.containsKey(id)) {
            logger.info("通道{}重复注册", id);
        } else {
            logger.debug("注册通道id为{}的通道{}", id, channel);
            this.pChannels.put(id, channel);
            publish(channel.id(), ProtocolEvent.REGISTER);
        }
    }

    @Override
    public void receive(byte[] data, String src) {
        logger.debug("接收到底层{}传来的数据，开始处理", src);
        publish(src, ProtocolEvent.RECEIVED, data);
        service.submit(() -> {
            try {
                PChannel channel = this.pChannels.get(src);
                ProtocolData protocolData = new ProtocolData(data, new ProtocolData.ChannelInfo(channel
                        .getRemoteHost(), channel.getPort(), channel.id()));
                byte type = protocolData.getData()[5];
                //只要收到消息就心跳一次
                channel.heartbeat();
                if (type == 0) {
                    logger.debug("数据报是心跳包，不处理");
                } else {
                    logger.debug("数据不是心跳包，将数据{}加入队列", protocolData);
                    this.deque.addLast(protocolData);
                }
            } catch (Exception e) {
                publish(src, ProtocolEvent.RECEIVEDERROR, e);
                logger.error("底层传来的数据处理过程中失败，数据为{}", data, e);
            }
        });
    }

    @Override
    public void close(String id, CloseCause cause) {
        logger.debug("关闭连接{}，关闭原因为：{}", id, cause);

        if (StringUtils.isEmpty(id)) {
            logger.warn("连接ID不能为null");
            return;
        }

        PChannel channel = this.pChannels.remove(id);
        if (channel == null) {
            logger.warn("要关闭的连接{}不存在", id);
            return;
        }
        channel.close();
        publish(id, ProtocolEvent.UNREGISTER, id, cause);
    }

    /**
     * 注册协议栈事件发布中心，该方法是比较低效的，请勿在运行时频繁调用
     *
     * @param eventCenter 事件中心
     */
    @Override
    public void register(@NonNull EventCenter eventCenter) {
        logger.debug("注册协议栈事件发布中心{}", eventCenter);
        this.eventCenters.add(eventCenter);
    }

    @Override
    public void publish(String channel, ProtocolEvent event, Object... args) {
        logger.debug("发布事件{}，事件参数为：{}", event, args);
        this.eventCenters.forEach(eventCenter -> {
            eventCenter.publish(channel, event, args);
        });
    }

    private ProtocolFuture send(ProtocolData protocolData) {
        logger.debug("接收到应用层的数据，开始处理。{}", protocolData);

        if (protocolData == null) {
            logger.warn("应用层发来的消息为空，不进行处理");
            return ProtocolFuture.ERRORFUTURE;
        }
        PChannel channel = this.pChannels.get(protocolData.getChannel());
        if (channel != null) {
            //先发布一个事件
            publish(channel.id(), ProtocolEvent.RECEIVEDSUCCESS, protocolData);
            logger.debug("找到了要发往的目的地{}的链接", protocolData.getChannel());
            try {
                ProtocolFuture channelFuture = channel.write(protocolData.getData());
                publish(channel.id(), ProtocolEvent.SEND, protocolData);
                return channelFuture;
            } catch (Throwable e) {
                logger.warn("发往目的地{}的数据发送失败", protocolData.getChannel(), e);
                return ProtocolFuture.ERRORFUTURE;
            }
        } else {
            logger.debug("应用层数据的目的地链接不存在");
            return ProtocolFuture.ERRORFUTURE;
        }
    }
}
