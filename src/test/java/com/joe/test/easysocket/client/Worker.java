package com.joe.test.easysocket.client;

import com.joe.test.easysocket.ext.Logger;
import com.joe.test.easysocket.ext.Serializer;

/**
 * @author joe
 */
public abstract class Worker {
    //序列化器
    Serializer serializer;
    //日志对象
    Logger logger;
    //当前服务器状态
    volatile boolean shutdown = true;
    //工作线程
    Thread worker;
    //关闭回调，系统关闭时会调用
    Callback callback;

    public Worker(Logger logger, Callback callback, Serializer serializer) {
        this.logger = logger;
        this.callback = callback;
        this.serializer = serializer;
    }

    /**
     * 关闭读取线程
     *
     * @return 返回true表示关闭成功，返回false表示当前已经关闭（重复关闭）
     */
    public synchronized boolean shutdown() {
        if (isShutdown()) {
            return false;
        }
        //必须先将shutdown置为true，work线程的正常中断依赖于该变量
        shutdown = true;
        worker.interrupt();
        callback.exec();
        return true;
    }

    /**
     * 获取当前工作器是否关闭
     *
     * @return 返回true表示已经关闭或者未启动，返回false表示未关闭
     */
    public boolean isShutdown() {
        return shutdown;
    }
}
