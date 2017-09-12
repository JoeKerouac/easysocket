package com.joe.test.easysocket.ext;

/**
 * Json序列化器
 *
 * @author joe
 */
public interface JsonParser {
    /**
     * 序列化对象
     *
     * @param obj 要序列化的对象
     * @return 序列化后的数据
     */
    String toJson(Object obj);
}
