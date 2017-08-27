package com.joe.easysocket.ext;

import com.joe.easysocket.protocol.ProtocolEvent;
import com.joe.easysocket.protocol.ProtocolEventListener;

/**
 * 事件中心
 *
 * @author joe
 */
public interface EventCenter {
    /**
     * 发布事件
     *
     * @param channel 事件所在channel
     * @param event   事件
     * @param args    事件的参数，可选
     */
    void publish(String channel, ProtocolEvent event, Object... args);

    /**
     * 注册事件监听器
     *
     * @param listener 事件监听器
     */
    void register(ProtocolEventListener listener);
}
