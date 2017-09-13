package com.joe.easysocket.server.common;

/**
 * 资源类，使用继承该接口的子类使用前必须调用init初始化，使用后必须调用销毁方法，同时
 * 需要保证初始化方法和销毁方法重复调用不会出错
 *
 * @author joe
 */
public interface Resource {
    /**
     * 初始化资源
     */
    void init();

    /**
     * 销毁资源
     */
    void destroy();
}
