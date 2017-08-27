package com.joe.easysocket.protocol;

/**
 * 事件监听器
 *
 * @author joe
 */
public interface ProtocolEventListener {
    /**
     * 是否关注该事件源
     *
     * @param channel 事件源的通道
     * @param event   事件源
     * @return 如果关注该事件源那么返回<code>true</code>，然后会调用exec方法处理该事件
     */
    boolean focus(String channel, ProtocolEvent event);

    /**
     * 处理事件
     *
     * @param channel 事件源的通道
     * @param event   事件源
     * @param args    该事件对应的参数
     */
    void exec(String channel, ProtocolEvent event, Object... args);
}
