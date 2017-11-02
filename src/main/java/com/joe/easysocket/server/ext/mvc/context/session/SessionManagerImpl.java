package com.joe.easysocket.server.ext.mvc.context.session;

import com.joe.easysocket.server.data.ProtocolData;
import com.joe.utils.concurrent.LockService;

import java.util.HashMap;
import java.util.Map;

/**
 * Session管理器
 *
 * @author joe
 */
public class SessionManagerImpl implements SessionManager {
    private Map<String, LocalSession> cache;

    @Override
    public void init() {
        cache = new HashMap<>();
    }

    @Override
    public void destroy() {
        cache.clear();
    }

    @Override
    public Session get(ProtocolData.ChannelInfo channel) {
        String id = channel.getChannel();
        if (cache.get(id) == null) {
            LockService.lock(id);
            if (cache.get(id) == null) {
                cache.put(id, new LocalSession(channel));
            }
            LockService.unlock(id);
        }
        return cache.get(id);
    }

    @Override
    public Session remove(ProtocolData.ChannelInfo channel) {
        return (channel == null || channel.getChannel() == null) ? null : cache.get(channel.getChannel());
    }
}
