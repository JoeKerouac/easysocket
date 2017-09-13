package com.joe.easysocket.server;

import com.joe.easysocket.server.common.Function;
import com.joe.easysocket.server.exception.ServerStartException;
import com.joe.easysocket.server.ext.EventCenter;
import com.joe.easysocket.server.ext.PublishCenter;
import com.joe.easysocket.server.protocol.Protocol;
import com.joe.easysocket.server.protocol.ProtocolImpl;
import com.joe.easysocket.server.protocol.ServerConfig;
import com.joe.easysocket.server.protocol.netty.ConnectorManager;
import com.joe.easysocket.server.protocol.netty.CustomFrameDecoder;
import com.joe.easysocket.server.protocol.netty.DatagramDecoder;
import com.joe.easysocket.server.protocol.netty.DatagramEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 服务器
 *
 * @author joe
 */
public interface Server {
    /**
     * 关闭服务器
     *
     * @param callback 关闭时服务器后的回调
     */
    void shutdown(Function callback);

    /**
     * 打开服务器
     *
     * @param callback 关闭回调，当使用kill命令关闭时会调用
     * @throws Exception 打开服务器中发生的异常
     */
    void start(Function callback) throws Exception;

    /**
     * 构建默认的netty实现的server
     *
     * @param config 服务器配置
     * @return 使用netty实现的server
     */
    static Server buildDefault(ServerConfig config) {
        return new NettyServer(config);
    }

    /**
     * 默认的基于netty实现的Server
     *
     * @author joe
     */
    class NettyServer implements Server {
        private static final Logger logger = LoggerFactory.getLogger(NettyServer.class);
        // 关闭锁
        private static final Object shutdownLock = new Object();
        // 是否是linux系统
        private static boolean linux;
        //当前服务器是否运行，只有调用start才会改变状态
        private AtomicBoolean start = new AtomicBoolean(false);
        // 接受请求的线程组，默认是机器核心的两倍
        private EventLoopGroup mainGroup;
        // 处理请求的线程组，默认是机器核心的两倍
        private EventLoopGroup workerGroup;

        // 监听端口
        private final int port;
        //协议栈
        private final Protocol protocol;
        //发布中心
        private final PublishCenter publishCenter;
        //事件中心
        private final List<EventCenter> eventCenters;
        //队列的最大长度
        private final int backlog;
        //是否延迟发送
        private final boolean nodelay;

        private NettyServer(ServerConfig serverConfig) {
            this.port = serverConfig.getPort() <= 0 ? 10051 : serverConfig.getPort();
            this.publishCenter = serverConfig.getPublishCenter();
            this.protocol = serverConfig.getProtocol() == null ? ProtocolImpl.builder().channel(serverConfig
                    .getChannel()).publishCenter(publishCenter).deque(serverConfig.getDeque()).heartbeaat(serverConfig.getHeartbeat()).build() : serverConfig
                    .getProtocol();
            if (serverConfig.getEventCenters() != null) {
                serverConfig.getEventCenters().forEach(this.protocol::register);
            }

            this.eventCenters = serverConfig.getEventCenters();
            this.backlog = serverConfig.getBacklog() <= 0 ? 512 : serverConfig.getBacklog();
            this.nodelay = serverConfig.isNodelay();
        }

        static {
            if (System.getProperty("os.name").contains("Linux")) {
                logger.debug("当前系统是linux");
                linux = true;
            } else {
                logger.debug("当前系统是windows");
                linux = false;
            }
        }

        /**
         * 关闭服务器
         *
         * @param callback 服务器关闭后的回调
         */
        @Override
        public synchronized void shutdown(Function callback) {
            if (!start.get()) {
                logger.debug("服务器已经关闭，请勿重复关闭");
            }
            logger.warn("服务器开始关闭................");
            logger.debug("开始关闭主线程组");
            mainGroup.shutdownGracefully();
            mainGroup = null;

            logger.debug("开始关闭工作线程组");
            workerGroup.shutdownGracefully();
            workerGroup = null;

            protocol.destroy();

            logger.warn("服务器关闭完成");
            start.set(false);
            if (callback != null) {
                callback.exec();
            }
        }

        /**
         * 启动服务器
         *
         * @param callback 关闭回调
         * @throws InterruptedException
         */
        @Override
        public synchronized void start(Function callback) throws InterruptedException {
            if (start.get()) {
                logger.warn("服务器已经在运行中，请勿重复启动");
            }

            try {
                start.set(true);
                logger.info("开始初始化服务器");
                //先初始化
                logger.debug("开始初始化服务器，初始化端口是：{}；是否延迟发送：{}；等待建立连接的队列长度为：{}", port, nodelay, backlog);
                protocol.init();
                eventCenters.forEach(protocol::register);
                // 初始化服务端
                ServerBootstrap bootstrap = new ServerBootstrap();
                DatagramDecoder datagramDecoder = new DatagramDecoder();
                DatagramEncoder datagramEncoder = new DatagramEncoder();

                if (linux) {
                    logger.debug("当前系统是linux系统，采用epoll模型");
                    mainGroup = new EpollEventLoopGroup();
                    workerGroup = new EpollEventLoopGroup();
                    bootstrap.channel(EpollServerSocketChannel.class);
                } else {
                    logger.debug("当前系统不是linux系统，采用nio模型");
                    mainGroup = new NioEventLoopGroup();
                    workerGroup = new NioEventLoopGroup();
                    bootstrap.channel(NioServerSocketChannel.class);
                }

                // 带child**的方法例如childHandler（）都是对应的worker线程组，不带child的对应的boss线程组
                bootstrap.group(mainGroup, workerGroup).childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        // 下边的编码解码器顺序不能变，CustomFrameDecoder必须每次都new，其他几个对象不用每次都new但是需要在类上加上@Sharable注解
                        ch.pipeline().addLast(new CustomFrameDecoder(), datagramDecoder, new ConnectorManager
                                (protocol), datagramEncoder);
                    }
                }).option(ChannelOption.SO_BACKLOG, backlog).childOption(ChannelOption
                        .TCP_NODELAY, nodelay);

                bootstrap.bind(port).sync();
                logger.info("监听端口是：{}", port);

                logger.debug("添加关闭监听");
                addCloseListener(callback);
                logger.debug("关闭监听添加完毕");
                logger.info("系统启动完成......");
            } catch (Exception e) {
                start.set(false);
                throw new ServerStartException(e);
            }
        }

        /**
         * 添加关闭监听，该监听可以监听kill PID，但是对kill -9 PID无效
         */
        private void addCloseListener(Function function) {
            // 该关闭监听针对kill PID
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.warn("系统即将关闭");
                shutdown(function);
            }));
        }
    }
}
