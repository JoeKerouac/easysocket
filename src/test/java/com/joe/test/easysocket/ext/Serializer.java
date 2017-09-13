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
    byte[] write(Object obj);

    /**
     * 反序列化对象
     *
     * @param data  对象数据
     * @param clazz 对象的Class
     * @param <T>   对象类型
     * @return 反序列化得到的对象
     */
    <T> T read(byte[] data, Class<T> clazz);
}
