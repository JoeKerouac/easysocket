package com.joe.test.easysocket;


import com.joe.easysocket.common.DatagramUtil;
import com.joe.easysocket.data.Datagram;

import java.io.InputStream;
import java.util.function.Consumer;

public class LengthFieldBasedFrameDecoder {
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
    public void read(InputStream in, Consumer<Datagram> consumer) throws Exception{
        try {
            //缓冲区
            byte[] buffer = new byte[this.bufferSize];
            //数据报长度，包含请求头
            int dataLen = 0;
            //当前写入指针
            int writePoint = 0;

            while (true) {
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
        } catch (Throwable e) {
            throw e;
        }
    }
}
