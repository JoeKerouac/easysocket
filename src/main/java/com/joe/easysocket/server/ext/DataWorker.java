package com.joe.easysocket.server.ext;

import com.joe.easysocket.server.common.Function;
import com.joe.easysocket.server.data.ProtocolData;
import com.joe.easysocket.server.ext.mvc.BeanContainer;
import com.joe.easysocket.server.ext.mvc.context.session.SessionManager;


/**
 * 协议栈数据处理器，bean容器和session管理器可以不注册，但是发布中心和队列必须注册；
 * 因为该处理器依赖这两个组件工作
 *
 * @author joe
 */
public interface DataWorker extends Runnable {
    /**
     * 启动，设计为通过该方法启动线程，用户也可以自己创建一个处理器然后放入线程池执行（建议通过该方法启动）
     * 注意：init方法不能用来启动
     *
     * @param name     线程名字后缀，如果为null则生成随机数
     * @param callback 系统关闭后的回调，系统启动起来如果调用shutdown或者使用kill
     *                 关闭系统会调用该函数
     * @return 返回线程的名字后缀
     */
    String start(String name, Function callback);

    /**
     * 关闭服务器
     */
    void shutdown();

    /**
     * 注册双向阻塞队列
     *
     * @param deque 双向阻塞队列
     */
    void register(CustomDeque<ProtocolData> deque);

    /**
     * 注册发布中心
     *
     * @param publishCenter 发布中心
     * @param channel       要监听的通道，监听通道默认为/protocol/receive（如果传入的通道为null或者空字符串）
     */
    void register(PublishCenter publishCenter, String channel);

    /**
     * 注册session管理器
     *
     * @param sessionManager session管理器
     */
    void register(SessionManager sessionManager);

    /**
     * 注册Bean容器
     *
     * @param beanContainer bean容器
     */
    void register(BeanContainer beanContainer);
}
