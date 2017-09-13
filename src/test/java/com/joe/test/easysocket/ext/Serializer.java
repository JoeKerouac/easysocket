package com.joe.test.easysocket.ext;

/**
 * 序列化器
 *
 * @author joe
 */
public interface Serializer {
    /**
     * 序列化对象
     *
     * @param obj 要序列化的对象
     * @return 序列化后的数据
     */
    String write(Object obj);
}
