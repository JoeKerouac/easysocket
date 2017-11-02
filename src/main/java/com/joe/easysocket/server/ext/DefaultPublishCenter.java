package com.joe.easysocket.server.ext;

import com.joe.utils.common.StringUtils;
import com.joe.utils.concurrent.LockService;
import com.joe.utils.parse.json.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 默认的发布中心（单机版，无法实现分布式，如需分布式的发布中心请自行实现）
 *
 * @author joe
 */
public class DefaultPublishCenter implements PublishCenter {
    private static final Logger logger = LoggerFactory.getLogger(DefaultPublishCenter.class);
    private static final Serializer DEFAULTSERIALIZER = new JsonSerializer();
    //监听者列表，key为channel，value为监听者
    private Map<String, Set<InternalMessageListener<?>>> customMessageListeners;
    //监听者反向缓存，其中key为监听者，value为监听通道，该值主要为了方便根据监听者删除用
    private Map<InternalMessageListener<?>, Set<String>> cache;
    private boolean init = false;
    //序列化器
    private Serializer serializer = DEFAULTSERIALIZER;

    public DefaultPublishCenter() {
        init();
    }

    @Override
    public void register(Serializer serializer) {
        this.serializer = serializer == null ? DEFAULTSERIALIZER : serializer;
        InternalMessageListener.serializer = serializer;
    }

    @Override
    public synchronized void init() {
        logger.debug("开始初始化DefaultPublishCenter");
        if (init) {
            logger.debug("DefaultPublishCenter已经初始化，不能重复初始化");
            return;
        }

        customMessageListeners = new ConcurrentHashMap<>();
        cache = new ConcurrentHashMap<>();

        logger.debug("DefaultPublishCenter初始化完成");
    }

    @Override
    public synchronized void destroy() {
        logger.debug("准备销毁DefaultPublishCenter");
        if (!init) {
            logger.debug("DefaultPublishCenter已经销毁或未初始化，不能销毁");
            return;
        }
        customMessageListeners.clear();
        logger.debug("DefaultPublishCenter销毁完成");
    }

    @Override
    public void pub(String channel, Object message) {
        logger.debug("为渠道{}发布消息：{}", channel, message);
        customMessageListeners.forEach((key, value) -> {
            if (key.equals(channel)) {
                value.forEach(listner -> {
                    listner.onMessage(channel.getBytes(), serializer.write(message));
                });
            }
        });
    }

    @Override
    public <T> void register(String channel, CustomMessageListener<T> listener) {
        logger.debug("为渠道{}注册监听者{}", channel, listener);
        InternalMessageListener<T> internalMessageListener = new InternalMessageListener(listener);

        //判断当前集合是否存在该channel，如果存在那么直接放入
        if (customMessageListeners.containsKey(channel)) {
            customMessageListeners.get(channel).add(internalMessageListener);
            cache.get(internalMessageListener).add(channel);
        } else {
            try {
                //加锁判断
                LockService.lock(channel);
                if (customMessageListeners.containsKey(channel)) {
                    customMessageListeners.get(channel).add(internalMessageListener);
                    cache.get(internalMessageListener).add(channel);
                } else {
                    //加锁后发现还没有，那么创建一个新的set
                    Set<InternalMessageListener<?>> listeners = new CustomSet<>();
                    customMessageListeners.put(channel, listeners);
                    listeners.add(internalMessageListener);

                    Set<String> channels = new CustomSet<>();
                    cache.put(internalMessageListener, channels);
                    channels.add(channel);
                }
            } finally {
                LockService.unlock(channel);
            }
        }
    }

    @Override
    public <T> void unregister(CustomMessageListener<T> listener) {
        logger.debug("删除监听者{}所有的监听", listener);
        if (listener == null) {
            return;
        }

        Set<String> keys = cache.get(listener);

        if (keys == null) {
            return;
        }

        keys.forEach(key -> {
            Set<InternalMessageListener<?>> listeners = customMessageListeners.get(key);
            listeners.remove(listener);
        });
        keys.clear();
    }

    @Override
    public <T> void unregister(String channel, CustomMessageListener<T> listener) {
        logger.debug("删除渠道{}下的监听者{}", channel, listener);
        if (StringUtils.isEmpty(channel) && listener == null) {
            return;
        }
        if (StringUtils.isEmpty(channel)) {
            //删除该监听者所有监听渠道
            unregister(listener);
        } else if (listener == null) {
            //删除该渠道的所有监听者
            unregister(channel);
        } else {
            //删除该渠道下的指定监听者
            Set<InternalMessageListener<?>> listeners = customMessageListeners.get(channel);
            if (listeners != null) {
                listeners.remove(listener);
                Set<String> channels = cache.get(listener);
                channels.remove(channel);
            }
        }
    }

    @Override
    public void unregister(String channel) {
        logger.debug("删除渠道{}下的所有监听者", channel);
        if (StringUtils.isEmpty(channel)) {
            logger.warn("要删除的渠道为空");
            return;
        }
        Set<InternalMessageListener<?>> listeners = customMessageListeners.get(channel);
        if (listeners != null) {
            listeners.forEach(listener -> {
                cache.get(listener).remove(channel);
            });
            listeners.clear();
        }
    }

    /**
     * 实际的消息监听者
     *
     * @param <T> 消息类型
     */
    private static class InternalMessageListener<T> {
        private static Serializer serializer = DEFAULTSERIALIZER;
        private CustomMessageListener<T> listener;

        private InternalMessageListener(CustomMessageListener<T> listener) {
            this.listener = listener;
        }

        /**
         * 处理消息
         *
         * @param channel 消息的渠道
         * @param message 消息
         */
        public void onMessage(byte[] channel, byte[] message) {

            try {
                T t = serializer.read(message, resolveMessageType());
                logger.debug("收到消息，解析出来的结果为：{}", t);
                listener.onMessage(channel, t);
            } catch (Exception e) {
                logger.warn("解析消息{}时出错，消息类型为：{}", message, resolveMessageType());
            }
        }

        /**
         * 返回消息的类型
         *
         * @return 消息的类型
         */
        public Class<T> resolveMessageType() {
            return listener.resolveMessageType();
        }

        @Override
        public int hashCode() {
            return listener.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }

            if (this == obj || this.listener == obj) {
                return true;
            }

            if (obj instanceof InternalMessageListener) {
                InternalMessageListener internalMessageListener = (InternalMessageListener) obj;
                return this.listener.equals(internalMessageListener.listener);
            } else if (obj instanceof CustomMessageListener) {
                CustomMessageListener listener = (CustomMessageListener) obj;
                return listener.equals(this.listener);
            } else {
                return false;
            }
        }
    }

    /**
     * 默认的json序列化器
     */
    private static class JsonSerializer implements Serializer {
        private static final JsonParser parser = JsonParser.getInstance();

        @Override
        public byte[] write(Object obj) {
            if (obj == null) {
                return null;
            } else if (obj instanceof byte[]) {
                return (byte[]) obj;
            } else if (obj instanceof String) {
                return ((String) obj).getBytes();
            }
            return parser.toJson(obj).getBytes();
        }

        @Override
        public <T> T read(byte[] data, Class<T> clazz) {
            if (data == null || clazz == null)
                return null;
            return parser.readAsObject(data, clazz);
        }

        @Override
        public boolean writeable(Object obj) {
            return true;
        }

        @Override
        public <T> boolean readable(Class<T> clazz) {
            return true;
        }
    }

    private static class CustomSet<E> implements Set<E> {
        private static final Object value = new Object();
        private ConcurrentHashMap<E, Object> concurrentHashMap;

        private CustomSet() {
            concurrentHashMap = new ConcurrentHashMap<>();
        }

        @Override
        public int size() {
            return concurrentHashMap.size();
        }

        @Override
        public boolean isEmpty() {
            return concurrentHashMap.isEmpty();
        }

        @Override
        public boolean contains(Object o) {
            return concurrentHashMap.containsKey(o);
        }

        @Override
        public Iterator<E> iterator() {
            return concurrentHashMap.keySet().iterator();
        }

        @Override
        public Object[] toArray() {
            return concurrentHashMap.keySet().toArray();
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return concurrentHashMap.keySet().toArray(a);
        }

        @Override
        public boolean add(E e) {
            concurrentHashMap.put(e, value);
            return true;
        }

        @Override
        public boolean remove(Object o) {
            return concurrentHashMap.remove(o) != null;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return concurrentHashMap.keySet().containsAll(c);
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            c.forEach(e -> {
                concurrentHashMap.put(e, value);
            });
            return true;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            concurrentHashMap.clear();
            c.forEach(e -> {
                try {
                    concurrentHashMap.put((E) e, value);
                } catch (Exception e1) {
                    logger.debug("替换出错", e1);
                }
            });
            return true;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            c.forEach(e -> {
                concurrentHashMap.remove(e);
            });
            return true;
        }

        @Override
        public void clear() {
            concurrentHashMap.clear();
        }
    }
}
