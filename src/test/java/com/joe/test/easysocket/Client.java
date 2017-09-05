package com.joe.test.easysocket;


import com.joe.easysocket.common.DatagramUtil;
import com.joe.easysocket.data.Datagram;
import com.joe.utils.StringUtils;
import lombok.Builder;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.function.Consumer;

/**
 * socket客户端
 *
 * @author joe
 */
public class Client {
    private String host;
    private int port;
    private Consumer<Datagram> consumer;
    private OutputStream out;
    private Socket socket;
    private int heartbeat;
    private Thread readThread;
    private Thread heartbeatThread;
    private volatile long lastHeartbeat;

    /**
     * 客户端构造器
     *
     * @param host      服务器地址
     * @param port      服务器端口
     * @param heartbeat 心跳时间（单位为秒）
     * @param consumer  数据处理器，处理服务器响应数据
     */
    @Builder
    private Client(String host, int port, int heartbeat, Consumer<Datagram> consumer) {
        if (StringUtils.isEmpty(host) || port <= 0)
            throw new IllegalArgumentException("参数错误");
        this.host = host;
        this.port = port;
        this.heartbeat = heartbeat <= 30 ? 30 : heartbeat;
        this.consumer = consumer;
    }

    public void start() throws IOException {
        lastHeartbeat = System.currentTimeMillis();
        this.socket = new Socket(host, port);
        LengthFieldBasedFrameDecoder coder = new LengthFieldBasedFrameDecoder();
        readThread = new Thread(() -> {
            try {
                coder.read(this.socket.getInputStream(), consumer);
            } catch (Throwable e) {
                e.printStackTrace();
                shutdown();
            }
        }, "socket读取线程");

        heartbeatThread = new Thread(() -> {
            try {
                while (true) {
                    if (System.currentTimeMillis() - lastHeartbeat >= (heartbeat * 1000)) {
                        byte[] data = DatagramUtil.build(null, (byte) 0, (byte) 1).getData();
                        write(data);
                        lastHeartbeat = System.currentTimeMillis();
                    }
                    Thread.sleep(1000);
                }
            } catch (Throwable e) {
                e.printStackTrace();
                shutdown();
            }
        }, "心跳线程");

        readThread.start();
        heartbeatThread.setDaemon(true);
        heartbeatThread.start();
        this.out = this.socket.getOutputStream();
    }

    public void write(byte[] data) throws IOException {
        lastHeartbeat = System.currentTimeMillis();
        this.out.write(data);
    }

    public void shutdown() {
        if (readThread != null) {
            readThread.interrupt();
            readThread = null;
        }
    }
}
