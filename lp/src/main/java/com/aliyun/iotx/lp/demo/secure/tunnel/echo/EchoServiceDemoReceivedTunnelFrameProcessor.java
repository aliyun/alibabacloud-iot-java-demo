package com.aliyun.iotx.lp.demo.secure.tunnel.echo;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.dto.TunnelFrame;
import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.handler.ReceivedTunnelFrameProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * received tunnel frame processor for this echo demo
 */
public class EchoServiceDemoReceivedTunnelFrameProcessor extends ReceivedTunnelFrameProcessor {
    private static final Logger log = LoggerFactory.getLogger(EchoServiceDemoReceivedTunnelFrameProcessor.class);

    private final String tunnelId;
    private final Map<String, Map<String, JSONObject>> sentMessageOfSession;

    public EchoServiceDemoReceivedTunnelFrameProcessor(String tunnelId, Map<String, Map<String, JSONObject>> sentMessageOfSession) {
        this.tunnelId = tunnelId;
        this.sentMessageOfSession = sentMessageOfSession;
    }

    @Override
    protected void handleDataTransport(TunnelFrame tunnelFrame) {
        Map<String, JSONObject> sentMessageMap = sentMessageOfSession.get(tunnelFrame.getFrameHeader().getSessionId());
        if (sentMessageMap == null) {
            log.error("sentMessageMap is null. session:{} tunnelId:{}", tunnelFrame.getFrameHeader().getSessionId(), tunnelId);
            return;
        }

        byte[] payloadBytes = tunnelFrame.getPayloadBytes();
        if (payloadBytes == null || payloadBytes.length == 0) {
            log.error("the payload of received tunnel frame is empty. session:{} tunnelId:{}", tunnelFrame.getFrameHeader().getSessionId(), tunnelId);
            return;
        }
        String payloadStr = new String(payloadBytes, StandardCharsets.UTF_8);
        JSONObject receivedJsonPayload = JSONObject.parseObject(payloadStr);
        if (!receivedJsonPayload.containsKey("key")) {
            log.error("the payload of received tunnel frame is invalid, corresponding json object has no attribute named key. session:{} tunnelId:{}",
                    tunnelFrame.getFrameHeader().getSessionId(), tunnelId);
            return;
        }

        JSONObject sentPayload = sentMessageMap.remove(receivedJsonPayload.getString("key"));

        if (sentPayload != null) {
            if (!isDataEqual(sentPayload, receivedJsonPayload)) {
                log.error("the payload of received tunnel frame is different with sent frame. frameId:{} session:{} tunnelId:{}",
                        tunnelFrame.getFrameHeader().getFrameId(), tunnelFrame.getFrameHeader().getSessionId(), tunnelId);
            }
        } else {
            log.error("the received tunnel frame has no corresponding sent frame. frameId:{} session:{} tunnelId:{}",
                    tunnelFrame.getFrameHeader().getFrameId(), tunnelFrame.getFrameHeader().getSessionId(), tunnelId);
        }
    }

    private boolean isDataEqual(JSONObject sentJsonPayload, JSONObject receivedJsonPayload) {
        if (sentJsonPayload == null || receivedJsonPayload == null) {
            return false;
        }
        return sentJsonPayload.equals(receivedJsonPayload);
    }
}
