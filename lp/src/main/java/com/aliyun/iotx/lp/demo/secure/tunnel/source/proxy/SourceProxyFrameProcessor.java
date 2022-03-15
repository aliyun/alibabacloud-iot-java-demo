package com.aliyun.iotx.lp.demo.secure.tunnel.source.proxy;

import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.dto.TunnelFrame;
import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.handler.ReceivedTunnelFrameProcessor;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * the processor to process data from device
 */
public class SourceProxyFrameProcessor extends ReceivedTunnelFrameProcessor {

    private static final Logger log = LoggerFactory.getLogger(SourceProxyFrameProcessor.class);

    private final String tunnelId;
    private final Map<String, Channel> channelOfSession;

    public SourceProxyFrameProcessor(String tunnelId, Map<String, Channel> channelOfSession) {
        this.tunnelId = tunnelId;
        this.channelOfSession = channelOfSession;
    }

    @Override
    protected void handleDataTransport(TunnelFrame tunnelFrame) {
        String sessionId = tunnelFrame.getFrameHeader().getSessionId();
        Channel channel = channelOfSession.get(sessionId);
        if (channel == null) {
            log.error("there is no channel for sessionId:{} tunnelId:{}", sessionId, tunnelId);
            return;
        }
        if (!channel.isActive()) {
            log.error("the channel is inactive for sessionId:{} tunnelId:{}", sessionId, tunnelId);
            return;
        }
        log.debug("receive data from device, sessionId:{} tunnelId:{}", sessionId, tunnelId);
        channel.writeAndFlush(Unpooled.wrappedBuffer(tunnelFrame.getPayloadBytes()));
    }
}
