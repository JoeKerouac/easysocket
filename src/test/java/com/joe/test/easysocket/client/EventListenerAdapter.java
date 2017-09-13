package com.joe.test.easysocket.client;

import java.net.Socket;

/**
 * 事件适配器
 *
 * @author joe
 */
public interface EventListenerAdapter extends EventListener {
    default void listen(SocketEvent event, Object... args) {
        switch (event) {
            case FAILD:
                faild((Throwable) args[0]);
                break;
            case REGISTER:
                register((Client) args[0]);
                break;
            case RECONNECT:
                reconnect((Client) args[0]);
                break;
            case UNREGISTER:
                unregister();
                break;
            default:
                throw new RuntimeException("没有监听[" + event + "]事件");
        }
    }

    /**
     * 连接失败，并且自动重连失败
     *
     * @param cause 失败原因
     */
    void faild(Throwable cause);

    /**
     * 连接注册成功
     *
     * @param client 注册成功后的client
     */
    void register(Client client);

    /**
     * 自动重连成功（由于底层只是自动建立连接，并没有重新登录等逻辑，所以需要用户自己实现重连后的逻辑，例如重新登录）
     *
     * @param client 重连后的client
     */
    void reconnect(Client client);

    /**
     * 通道关闭（用户主动调用shutdown）
     */
    void unregister();
}
