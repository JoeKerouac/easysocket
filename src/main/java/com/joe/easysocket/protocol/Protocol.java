package com.joe.easysocket.protocol;

import com.joe.easysocket.common.Resource;
import com.joe.easysocket.data.ProtocolData;
import com.joe.easysocket.ext.CustomDeque;
import com.joe.easysocket.ext.EventCenter;
import com.joe.easysocket.ext.PublishCenter;

/**
 * 协议栈定义，需要提供以下功能：
 * 1、可以从发布中心获取应用层到底层的数据（为应用层提供可以直接发送消息到指定通道
 * 的可能）。
 * 监听通道默认为/protocol/receive，
 * 数据类型为ProtocolData
 * 2、协议栈需要可以处理心跳包，对于无效连接需要定期清除
 * <p>
 * 该协议栈的收发模型描述如下：
 * 1、协议栈和应用层都可以有多个；
 * 2、使用协议栈时需要注册一个队列和发布中心（同时指定监听通道，默认为/protocol/receive），不注册不能使用；
 * 3、协议栈通过队列向应用层发布数据，也就是应用层需要能够读取到该队列的信息，同时队列保证消息只能被读取一次；
 * 4、应用层通过发布中心向协议栈发布数据，所有协议栈注册相应的监听通道监听消息，然后判断该消息要发往的目的地是否在本协议栈，如
 * 过是则发送，如果不是则不处理；
 * 5、如果有多个协议栈或者应用层需要保证队列和发布中心是唯一的，也就是所有的协议栈和发布中心共享同一个（可以是同一个集群）队列和发布中心；
 *
 * @author joe
 */
public interface Protocol extends Resource {
    /**
     * 协议栈从底层接收数据，然后交由应用层处理
     *
     * @param data 底层传过来的数据
     * @param src  数据来源
     */
    void receive(byte[] data, String src);

    /**
     * 关闭指定数据通道
     *
     * @param id    通道的ID
     * @param cause 关闭原因
     */
    void close(String id, CloseCause cause);

    /**
     * 注册事件中心
     *
     * @param eventCenter 事件中心
     */
    void register(EventCenter eventCenter);

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
     * 注册链接
     *
     * @param channel 要注册的链接
     */
    void register(PChannel channel);

    /**
     * 发布事件
     *
     * @param channel 事件源的channel
     * @param event   事件
     * @param args    事件的参数，可选
     */
    void publish(String channel, ProtocolEvent event, Object... args);
}
