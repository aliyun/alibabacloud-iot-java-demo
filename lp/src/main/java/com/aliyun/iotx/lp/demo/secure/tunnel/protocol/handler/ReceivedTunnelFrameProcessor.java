package com.aliyun.iotx.lp.demo.secure.tunnel.protocol.handler;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.common.ChannelAttributeKeyConstant;
import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.dto.TunnelFrame;
import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.dto.TunnelFrameType;
import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.dto.TunnelResponse;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * received tunnel frame processor
 */
public abstract class ReceivedTunnelFrameProcessor {
    private static final Logger log = LoggerFactory.getLogger(ReceivedTunnelFrameProcessor.class);

    /**
     * frameId -> TunnelFrameCallback
     */
    private final Map<Long, TunnelFrameCallback> callbacks = new ConcurrentHashMap<>();

    public void addFrameCallback(Long frameId, TunnelFrameCallback callback) {
        Objects.requireNonNull(frameId);
        Objects.requireNonNull(callback);
        callbacks.put(frameId, callback);
    }


    public TunnelFrameCallback removeFrameCallback(Long frameId) {
        Objects.requireNonNull(frameId);
        return callbacks.remove(frameId);
    }

    /**
     * handle received tunnel frame
     *
     * @param channel     WebSocket channel
     * @param tunnelFrame tunnel frame
     */
    void handleFrame(Channel channel, TunnelFrame tunnelFrame) {
        TunnelFrameType frameType = TunnelFrameType.typeOf(tunnelFrame.getFrameHeader().getFrameType());
        String tunnelId = channel.attr(ChannelAttributeKeyConstant.ATTRIBUTE_KEY_TUNNEL_ID).get();

        switch (frameType) {
            case SESSION_CREATE:
                onSessionCreate(tunnelId, tunnelFrame);
                return;
            case SESSION_RELEASE:
                onSessionRelease(tunnelId, tunnelFrame);
                return;
            case COMMON_RESPONSE:
                onResponse(tunnelId, tunnelFrame);
                return;
            case DATA_TRANSPORT:
                onDataTransport(tunnelId, tunnelFrame);
                return;
            default:
                throw new UnsupportedOperationException("unexpected messageType='" + frameType + "' tunnelId:" + tunnelId);
        }
    }

    private void onDataTransport(String tunnelId, TunnelFrame tunnelFrame) {
        log.debug("source receive data-trans-frame, tunnelId:{} tunnelFrameHeader:{}", tunnelId,
                tunnelFrame.getFrameHeader().toReadableJSONString());
        handleDataTransport(tunnelFrame);
    }

    /**
     * handle the data transport frame
     *
     * @param tunnelFrame the data transport frame
     */
    protected abstract void handleDataTransport(TunnelFrame tunnelFrame);

    private void onSessionRelease(String tunnelId, TunnelFrame tunnelFrame) {
        log.info("source receive session-release-frame, tunnelFrameHeader:{}", tunnelFrame.getFrameHeader().toReadableJSONString());

        byte[] payloadBytes = tunnelFrame.getPayloadBytes();
        if (payloadBytes == null || payloadBytes.length == 0) {
            log.error("the payload of session release frame is empty. tunnelId:{} sessionId:{}", tunnelId, tunnelFrame.getFrameHeader().getSessionId());
            return;
        }

        JSONObject jsonObject = JSONObject.parseObject(new String(payloadBytes, StandardCharsets.UTF_8));
        this.handleSessionRelease(tunnelFrame.getFrameHeader().getSessionId(), jsonObject.getString("code"), jsonObject.getString("msg"));
    }

    protected void handleSessionRelease(String sessionId, String code, String msg) {

    }

    private void onSessionCreate(String tunnelId, TunnelFrame tunnelFrame) {
        log.error("source should not receive session-create-frame, tunnelId:{} tunnelFrameHeader:{}",
                tunnelId, tunnelFrame.getFrameHeader().toReadableJSONString());
    }

    private void onResponse(String tunnelId, TunnelFrame tunnelFrame) {
        Long frameId = tunnelFrame.getFrameHeader().getFrameId();
        if (frameId == null) {
            throw new RuntimeException("frameId can not be null");
        }

        TunnelResponse response = TunnelResponse.parse(tunnelFrame.content());

        String messageHeaderJson = tunnelFrame.getFrameHeader().toReadableJSONString();
        if (!response.isSuccess()) {
            log.error("receive failed response, messageHeader={}; response={}",
                    messageHeaderJson, response.toAbstractInfo());
        } else {
            log.info("receive success response, messageHeader={}; response={}",
                    messageHeaderJson, response.toAbstractInfo());
        }

        TunnelFrameCallback messageCallBack = removeFrameCallback(frameId);

        if (messageCallBack == null) {
            log.debug("message miss callback, messageHeader={}; response={}",
                    messageHeaderJson, response.toAbstractInfo());

            return;
        }

        if (response.isSuccess()) {
            try {
                messageCallBack.onSuccess(tunnelFrame, response);
            } catch (Throwable e) {
                log.error("message callback 'onSuccess' catch exception, messageHeader={}; errorMsg={}",
                        messageHeaderJson, e.getMessage(), e);
            }
        } else {
            try {
                messageCallBack.onFailure(tunnelFrame, response);
            } catch (Throwable e) {
                log.error("message callback 'onFailure' catch exception, messageHeader={}; errorMsg={}",
                        messageHeaderJson, e.getMessage(), e);
            }
        }

    }
}
