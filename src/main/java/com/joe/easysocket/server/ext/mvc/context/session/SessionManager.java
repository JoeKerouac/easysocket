package com.joe.easysocket.server.ext.mvc.context.session;

import com.joe.easysocket.server.common.Resource;
import com.joe.easysocket.server.data.ProtocolData;

/**
 * session管理器
 *
 * @author joe
 */
public interface SessionManager extends Resource {
    /**
     * 获取指定channel信息对应的session，如果没有就创建返回
     *
     * @param channel 指定channel信息
     * @return 指定channel对应的session
     */
    Session get(ProtocolData.ChannelInfo channel);

    /**
     * 删除session
     *
     * @param channel 要删除的session对应的通道信息
     * @return 删除的session
     */
    Session remove(ProtocolData.ChannelInfo channel);
}
