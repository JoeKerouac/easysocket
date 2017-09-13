package com.joe.easysocket.server.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

/**
 * 协议栈数据，协议栈处理数据的最小单位
 *
 * @author joe
 */
@lombok.Data
@AllArgsConstructor
public class ProtocolData {
    //应用层数据报
    private byte[] data;
    //数据对应的通道ID
    private @NonNull
    ChannelInfo channelInfo;

    @JsonIgnore
    public String getChannel() {
        return channelInfo.getChannel();
    }

    @JsonIgnore
    public int getRemotePort() {
        return channelInfo.getPort();
    }

    @JsonIgnore
    public String getRemoteHost() {
        return channelInfo.getHost();
    }

    @JsonIgnore
    public String id() {
        return channelInfo.getChannel();
    }

    /**
     * 通道信息
     *
     * @author joe
     */
    @AllArgsConstructor
    @ToString
    public static final class ChannelInfo {
        @Getter
        private String host = null;
        @Getter
        private int port = -1;
        @Getter
        private String channel;

        public ChannelInfo(@NonNull String channel) {
            this.channel = channel;
        }
    }
}
