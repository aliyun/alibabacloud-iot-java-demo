package com.aliyun.iotx.lp.demo.secure.tunnel.protocol.handler;

import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.dto.TunnelFrame;
import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.dto.TunnelFrameHeader;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.handler.codec.http.websocketx.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * handler to handle the received and sent tunnel frame
 */
public class DeviceSecureTunnelFrameHandler extends MessageToMessageCodec<WebSocketFrame, TunnelFrame> {
    private static final Logger log = LoggerFactory.getLogger(DeviceSecureTunnelFrameHandler.class);

    private final ReceivedTunnelFrameProcessor deviceSecureTunnelFrameProxy;

    public DeviceSecureTunnelFrameHandler(ReceivedTunnelFrameProcessor deviceSecureTunnelFrameProxy) {
        this.deviceSecureTunnelFrameProxy = deviceSecureTunnelFrameProxy;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, WebSocketFrame msg, List<Object> out) {
        ByteBuf byteBuf = readIntactWebSocketFrame(ctx, msg);
        // haven't received finish frame
        if (byteBuf == null) {
            return;
        }

        TunnelFrame tunnelFrame;
        TunnelFrameHeader tunnelFrameHeader = null;
        String tunnelFrameHeaderJson = null;
        try {
            tunnelFrame = TunnelFrame.parse(byteBuf);
            tunnelFrameHeader = tunnelFrame.getFrameHeader();

            deviceSecureTunnelFrameProxy.handleFrame(ctx.channel(), tunnelFrame);
        } catch (Throwable e) {
            if (tunnelFrameHeader != null) {
                tunnelFrameHeaderJson = tunnelFrameHeader.toReadableJSONString();
            }
            log.error("unexpected error; messageHeader={}; errorMsg={}", tunnelFrameHeaderJson, e.getMessage(), e);
        }
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, TunnelFrame tunnelFrame, List<Object> out) {
        tunnelFrame.retain();
        ByteBuf binaryData = tunnelFrame.frameToByteBuf();
        out.add(new BinaryWebSocketFrame(binaryData));
    }

    protected ByteBuf readIntactWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame webSocketFrame) {
        if (!webSocketFrame.isFinalFragment()) {
            log.error("receive not final 'WebSocketFrame', not supported.");
            ctx.channel().close();
            return null;
        }

        ByteBuf currentFragmentByteBuf;
        if (webSocketFrame instanceof TextWebSocketFrame) {
            log.info("receive 'TextWebSocketFrame', not supported.");
            throw new UnsupportedOperationException("unsupported WebSocketFrame's type, type='" + webSocketFrame.getClass() + "'");
        } else if (webSocketFrame instanceof ContinuationWebSocketFrame) {
            log.info("receive 'ContinuationWebSocketFrame', not supported.");
            throw new UnsupportedOperationException("unsupported WebSocketFrame's type, type='" + webSocketFrame.getClass() + "'");
        } else if (webSocketFrame instanceof BinaryWebSocketFrame) {
            BinaryWebSocketFrame binaryWebSocketFrame = (BinaryWebSocketFrame) webSocketFrame;
            currentFragmentByteBuf = binaryWebSocketFrame.content().retain();
        } else if (webSocketFrame instanceof PingWebSocketFrame) {
            log.info("receive 'PingWebSocketFrame'.");
            return null;
        } else if (webSocketFrame instanceof PongWebSocketFrame) {
            log.info("receive 'PongWebSocketFrame'.");
            return null;
        } else if (webSocketFrame instanceof CloseWebSocketFrame) {
            log.info("receive webSocket 'CloseWebSocketFrame'.");
            ctx.channel().close();
            return null;
        } else {
            throw new UnsupportedOperationException("unsupported WebSocketFrame's type, type='" + webSocketFrame.getClass() + "'");
        }
        return currentFragmentByteBuf;
    }
}
