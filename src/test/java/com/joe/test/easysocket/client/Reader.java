package com.joe.test.easysocket.client;

import com.joe.easysocket.common.DatagramUtil;
import com.joe.easysocket.data.Datagram;
import com.joe.easysocket.ext.dataworker.mvc.data.InterfaceData;
import com.joe.test.easysocket.ext.InternalLogger;
import com.joe.test.easysocket.ext.Logger;
import com.joe.test.easysocket.ext.Serializer;

import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * socket数据读取器
 *
 * @author joe
 */
public class Reader extends Worker {
    // 数据报head中长度字段的起始位置（从0开始）
    private final int lengthFieldOffset = 1;
    // 数据报head的长度
    private final int headLength = 16;
    //事件监听器
    private EventListener listener;
    //socket输入流
    private InputStream input;
    //缓冲区大小
    private int bufferSize;
    //线程池
    private ExecutorService service = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);


    /**
     * 读取器构造器
     *
     * @param input    socket的输入流
     * @param logger   日志对象
     * @param listener 事件监听器
     */
    public Reader(@NotNull InputStream input, @NotNull Logger logger, @NotNull EventListener listener, Callback
            callback,
                  Serializer serializer) {
        super(logger instanceof InternalLogger ? logger : InternalLogger.getLogger(logger, Reader.class), callback,
                serializer);
        this.input = input;
        this.listener = listener;
        this.bufferSize = 1024;
    }

    /**
     * 启动数据读取器
     */
    public synchronized void start() {
        if (!isShutdown()) {
            throw new IllegalThreadStateException("请勿重复启动读取器");
        }
        shutdown = false;
        this.worker = new Thread(() -> {
            logger.info("数据读取器启动");
            try {
                read();
            } catch (IOException e) {
                logger.debug("读取器输入流读取发生异常，中断工作" + e);
            } catch (InterruptedException e) {
                logger.debug("读取器被外部中断，停止工作");
            } finally {
                if (!isShutdown()) {
                    shutdown();
                }
            }
        }, "数据读取器");
        this.worker.start();
    }


    /**
     * 从socket输入流中读取
     *
     * @throws IOException
     */
    private void read() throws IOException, InterruptedException {
        //缓冲区
        byte[] buffer = new byte[this.bufferSize];
        //数据报长度，包含请求头
        int dataLen = 0;
        //当前写入指针
        int writePoint = 0;

        while (!isShutdown()) {
            int readLen = input.read(buffer, writePoint, buffer.length - writePoint);
            if (readLen == -1) {
                logger.error("socket输入流被关闭，读取结束");
                shutdown();
            }
            writePoint += readLen;

            if (writePoint >= headLength) {
                dataLen = DatagramUtil.convert(buffer, lengthFieldOffset) + headLength;
                logger.debug("读取到数据报的长度，数据报长度为：" + dataLen);
            }

            if (writePoint >= dataLen) {
                logger.debug("本次数据报数据读取完毕");
                //完整的数据报
                Datagram datagram = DatagramUtil.decode(buffer);
                logger.debug("读取到的数据报为：" + datagram);
                System.arraycopy(buffer, dataLen, buffer, 0, buffer.length - dataLen);
                //重置
                dataLen = 0;
                writePoint = 0;

                if (datagram.getType() == 0) {
                    logger.debug("收到的数据报为心跳包，忽略处理");
                } else if (datagram.getType() == 1) {
                    logger.debug("将数据报[" + datagram + "]提交到线程池处理");

                    service.submit(() -> {
                        logger.debug("开始解析数据报[" + datagram + "]的body");
                        InterfaceData data = serializer.read(datagram.getBody(), InterfaceData.class);
                        logger.debug("解析出来的数据报body为：" + data);
                        listener.listen(SocketEvent.RECEIVE, data);
                    });
                }
            } else if (dataLen > buffer.length) {
                //扩容
                if (buffer.length >= Integer.MAX_VALUE) {
                    throw new OutOfMemoryError("已经扩容至最大，无法继续扩容");
                }
                int newLen = buffer.length + Math.min(buffer.length / 2, 2048);
                if (newLen < 0) {
                    //越界
                    newLen = Integer.MAX_VALUE;
                }
                byte[] newBuffer = new byte[newLen];
                System.arraycopy(buffer, 0, newBuffer, 0, writePoint);
                buffer = newBuffer;
            }
        }
    }

    @Override
    public synchronized boolean shutdown() {
        if (super.shutdown()) {
            this.service.shutdown();
            return true;
        } else {
            return false;
        }
    }
}
