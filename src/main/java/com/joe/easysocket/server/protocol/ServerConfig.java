package com.joe.easysocket.server.protocol;

import com.joe.easysocket.server.ext.CustomDeque;
import com.joe.easysocket.server.ext.EventCenter;
import com.joe.easysocket.server.ext.PublishCenter;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

/**
 * 服务器配置
 *
 * @author joe
 */
@Builder
@Data
public class ServerConfig {
    // 监听端口
    @Builder.Default
    private int port = 10051;
    //队列的最大长度
    @Builder.Default
    private int backlog = 512;
    //是否延迟发送
    @Builder.Default
    private boolean nodelay = true;
    //协议栈
    private Protocol protocol;
    //发布中心
    private PublishCenter publishCenter;
    //发布中心接收上层消息的通道
    private String channel;
    //事件中心
    @Singular
    private List<EventCenter> eventCenters;
    //队列
    private CustomDeque deque;
    //心跳周期，单位为秒，最短30秒
    @Builder.Default
    private int heartbeat = 30;
}
