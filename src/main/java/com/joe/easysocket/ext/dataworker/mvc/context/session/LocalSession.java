package com.joe.easysocket.ext.dataworker.mvc.context.session;


import com.joe.easysocket.data.ProtocolData;

import java.util.Map;
import java.util.TreeMap;

/**
 * 本地session，服务器关闭session内容就会丢失
 *
 * @author joe
 */
public class LocalSession implements Session {
    // session对应的channel
    private ProtocolData.ChannelInfo channelInfo;
    // 会话缓存
    private Map<String, Object> cache;

    public LocalSession(ProtocolData.ChannelInfo channelInfo) {
        this.cache = new TreeMap<>();
        this.channelInfo = channelInfo;
    }

    @Override
    public String getRemoteHost() {
        return channelInfo.getHost();
    }

    @Override
    public int getRemotePort() {
        return channelInfo.getPort();
    }

    @Override
    public String getId() {
        return channelInfo.getChannel();
    }

    @Override
    public void setAttribute(String key, Object value) {
        cache.put(key, value);
    }

    @Override
    public Object getAttribute(String key) {
        return cache.get(key);
    }

}
