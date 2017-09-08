package com.joe.test.easysocket.client;

import com.joe.easysocket.common.DatagramUtil;
import com.joe.easysocket.data.Datagram;
import com.joe.test.easysocket.ext.InternalLogger;
import com.joe.test.easysocket.ext.Logger;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.function.Consumer;

/**
 * @author joe
 */
public class Client {
    private LengthFieldBasedFrameDecoder decoder;
    private Socket socket;
    private volatile boolean shutdown = true;
    private volatile boolean faild = false;
    private String host;
    private int port;
    private Thread read;
    private Thread write;
    private Logger logger;
    private BlockingDeque<Msg> queue;
    private EventListener listener;
    private InputStream input;
    private OutputStream out;
    private Consumer<Datagram> consumer;
    private Throwable cause;

    @Builder
    public Client(@NotNull String host, int port, @NotNull Logger logger, @NotNull Consumer<Datagram>
            consumer) {
        this.host = host;
        this.port = port;
        this.logger = logger instanceof InternalLogger ? logger : InternalLogger.getLogger(logger, Client.class);
        this.queue = new LinkedBlockingDeque<>();
        this.consumer = consumer;
        this.decoder = new LengthFieldBasedFrameDecoder();
    }

    public void start() {
        try {
            socket = new Socket(host, port);
            input = socket.getInputStream();
            out = socket.getOutputStream();
        } catch (IOException e) {
            logger.error("连接构建时发生异常");
            logger.error(e.toString());
            return;
        }

        //读取线程
        read = new Thread(() -> {
            try {
                decoder.read(input, consumer);
            } catch (IOException e) {
                logger.warn("客户端读取异常，客户端关闭");
                logger.warn(e.toString());
                cause = e;
                faild = true;
            }
        });

        //发送线程
        write = new Thread(() -> {
            try {
                write(out);
            } catch (InterruptedException e) {
                //线程被中断，ignore
            } catch (IOException e) {
                cause = e;
                logger.warn("客户端发送异常，客户端关闭");
                logger.warn(e.toString());
                faild = true;
            }
        });
    }

    /**
     * 连接是否打开
     *
     * @return 连接打开可写返回true，否则返回false
     */
    private boolean isOpen() {
        return !shutdown && !faild;
    }

    /**
     * 写数据（从队列读取，只要连接没有关闭就一直写）
     *
     * @param out
     * @throws InterruptedException
     * @throws IOException
     */
    private void write(OutputStream out) throws InterruptedException, IOException {
        Msg msg = null;
        while (isOpen()) {
            try {
                msg = queue.take();
                logger.debug("收到消息：" + msg + "，准备发往服务器");
                Datagram datagram = DatagramUtil.build(msg.getData(), (byte) 1, (byte) 1);
                logger.debug("消息封装为数据报后是：" + datagram);
                out.write(datagram.getBody());
            } catch (IOException | InterruptedException e) {
                //防止发送失败丢失数据
                if (msg != null) {
                    queue.put(msg);
                }
                throw e;
            }
        }
    }

    /**
     * 往服务器发送数据
     *
     * @param invoke 要调用的接口名
     * @param data   要发送的接口数据
     * @return 返回true表示发送成功（并没有真正发送成功，只是加入了发送队列）
     */
    public boolean write(String invoke, byte[] data) {
        try {
            queue.put(new Msg(invoke, data));
            return true;
        } catch (InterruptedException e) {
            logger.error("写入失败");
            return false;
        }
    }

    public boolean isShutdown() {
        return shutdown;
    }

    private class LengthFieldBasedFrameDecoder {
        // 数据报head中长度字段的起始位置（从0开始）
        private int lengthFieldOffset;
        // 数据报head中长度字段的长度
        private int lengthFieldLength;
        // 数据报head的长度
        private int headLength;
        // 数据报最大长度（包含消息head和body）
        private int maxFrameLength;
        //缓冲区大小
        private int bufferSize;

        /**
         * @param maxFrameLength    数据报最大长度（包含消息head和body）
         * @param lengthFieldOffset 数据报head中长度字段的起始位置（从0开始）
         * @param lengthFieldLength 数据报head中长度字段的长度
         * @param headLength        数据报head的长度
         */
        public LengthFieldBasedFrameDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength,
                                            int headLength) {
            this.maxFrameLength = maxFrameLength;
            this.lengthFieldOffset = lengthFieldOffset;
            this.lengthFieldLength = lengthFieldLength;
            this.headLength = headLength;
            this.bufferSize = Math.min(maxFrameLength, 2048);
        }

        /**
         * 版本1时的默认构造，后续可能会变
         */
        public LengthFieldBasedFrameDecoder() {
            this(Datagram.MAX_LENGTH, 1, 4, 16);
        }


        /**
         * 从流中读取
         *
         * @param in       要读取的流
         * @param consumer 读取出来的数据报的处理方式
         */
        public void read(InputStream in, Consumer<Datagram> consumer) throws IOException {
            try {
                //缓冲区
                byte[] buffer = new byte[this.bufferSize];
                //数据报长度，包含请求头
                int dataLen = 0;
                //当前写入指针
                int writePoint = 0;

                while (isOpen()) {
                    int readLen = in.read(buffer, writePoint, buffer.length - writePoint);
                    writePoint += readLen;

                    if (writePoint >= headLength) {
                        dataLen = DatagramUtil.convert(buffer, lengthFieldOffset) + headLength;
                    }

                    if (writePoint >= dataLen) {
                        //完整的数据报
                        Datagram datagram = DatagramUtil.decode(buffer);
                        System.arraycopy(buffer, dataLen, buffer, 0, buffer.length - dataLen);
                        //重置
                        dataLen = 0;
                        writePoint = 0;
                        consumer.accept(datagram);
                    } else if (dataLen > buffer.length) {
                        //扩容
                        if (buffer.length >= Integer.MAX_VALUE) {
                            throw new OutOfMemoryError("已经扩容至最大，无法继续扩容");
                        }
                        int newLen = buffer.length + Math.min(buffer.length / 2, 2048);
                        if (newLen < 0) {
                            newLen = Integer.MAX_VALUE;
                        }
                        byte[] newBuffer = new byte[newLen];
                        System.arraycopy(buffer, 0, newBuffer, 0, writePoint);
                        buffer = newBuffer;
                    }
                }
            } catch (IOException e) {
                throw e;
            }
        }
    }


    @Data
    @AllArgsConstructor
    private static class Msg {
        private String invoke;
        private byte[] data;
    }
}
