package com.aliyun.iotx.lp.demo.secure.tunnel.source.proxy;

import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.source.TunnelSource;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.AttributeKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * handler for tcp connection from client
 */
public class SourceProxyChannelHandler extends SimpleChannelInboundHandler<ByteBuf> {

    private static final Logger log = LoggerFactory.getLogger(SourceProxyChannelHandler.class);

    private final String tunnelId;
    private final TunnelSource tunnelSource;
    private final static AttributeKey<String> CHANNEL_ATT_SESSION_ID = AttributeKey.valueOf("attribute_key_session_id");

    public SourceProxyChannelHandler(String tunnelId, TunnelSource tunnelSource) {
        this.tunnelId = tunnelId;
        this.tunnelSource = tunnelSource;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
        if (msg != null && msg.readableBytes() > 0) {
            if (!tunnelSource.isConnected()) {
                log.error("the tunnel source is disconnected. tunnelId:{}", tunnelId);
                return;
            }
            String sessionId = ctx.channel().attr(CHANNEL_ATT_SESSION_ID).get();
            if (sessionId == null || sessionId.length() == 0) {
                log.error("the sessionId is null or empty. tunnelId:{}, channelId:{}", tunnelId, ctx.channel().id());
                return;
            }
            byte[] rxBytes = new byte[msg.readableBytes()];
            msg.readBytes(rxBytes);
            tunnelSource.sendData(sessionId, rxBytes);
        } else {
            log.error("the msg is empty. tunnelId:{}, channelId:{}", tunnelId, ctx.channel().id());
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
        String sessionId = ctx.channel().attr(CHANNEL_ATT_SESSION_ID).get();
        if (sessionId == null || sessionId.length() == 0) {
            log.error("the sessionId is null or empty. tunnelId:{}, channelId:{}", tunnelId, ctx.channel().id());
            return;
        } else {
            tunnelSource.closeSession(sessionId);
        }
    }
}
