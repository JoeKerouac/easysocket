package com.joe.easysocket.protocol.netty;

import com.joe.easysocket.protocol.CloseCause;
import com.joe.easysocket.protocol.Protocol;
import com.joe.easysocket.protocol.ProtocolEvent;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 连接注册、注销管理
 *
 * @author joe
 */
public class ConnectorManager extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ConnectorManager.class);
    //当前连接的读取缓冲
    private ByteBuf byteBuf;
    private Protocol protocol;

    public ConnectorManager(Protocol protocol) {
        this.protocol = protocol;
    }

    /**
     * 管理通道的注册
     */
    @Override
    public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
        // 注册IO通道
        protocol.register(new NettyChannel(ctx.channel()));
        super.channelRegistered(ctx);
    }

    /**
     * 管理通道的注销，用户正常注销会调用该方法，同时当通道IO异常时也会调用该方法注销通道，用户正常注销时
     */
    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        logger.debug("注销通道{}", ctx.channel());
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 从IO通道读取信息
        logger.debug("包装请求信息，要包装的信息为：{}", msg);
        String id = ctx.channel().id().asLongText();
        ByteBufRef ref = (ByteBufRef) msg;
        byteBuf = ref.getByteBuf();
        protocol.receive(ref.getData(), id);
        // 将请求传递到处理链的下一个处理器，如果没有这一行则将终止处理
        super.channelRead(ctx, msg);
    }

    /**
     * IO异常时会调用该方法，不能在此方法内注销IO通道，IO通道会在channelRegistered方法中注销
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.warn("连接异常，关闭连接{}", ctx.channel().id().asLongText(), cause);
        protocol.close(ctx.channel().id().asLongText(), CloseCause.SYSTEM);
    }

    /**
     * 通道关闭时会调用该方法（无论是什么情况下关闭的都会调用该方法）
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String id = ctx.channel().id().asLongText();
        logger.debug("关闭通道{}", id);
        if (byteBuf == null || byteBuf.readableBytes() < 1) {
            logger.debug("通道{}注销，并且该通道没有未读消息", id);
        } else {
            logger.info("通道{}注销，并且存在未读消息，发布一个事件", id);
            byte[] discard = new byte[byteBuf.readableBytes()];
            byteBuf.readBytes(discard);
            protocol.publish(id, ProtocolEvent.DISCARD, id, discard);
        }
        // 连接断开时
        super.channelInactive(ctx);
    }
}
