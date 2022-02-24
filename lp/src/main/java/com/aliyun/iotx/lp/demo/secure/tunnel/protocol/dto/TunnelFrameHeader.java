package com.aliyun.iotx.lp.demo.secure.tunnel.protocol.dto;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.annotation.JSONField;

import java.util.HashMap;
import java.util.Map;

/**
 * the header of tunnel frame
 */
public class TunnelFrameHeader {
    public TunnelFrameHeader() {
    }

    public TunnelFrameHeader(Long frameId, int frameType, String serviceType, String sessionId) {
        this.frameId = frameId;
        this.frameType = frameType;
        this.serviceType = serviceType;
        this.sessionId = sessionId;
    }

    /**
     * tunnel frame id
     * this id should be unique for one tunnel during a time sliding window whose size is 1 minute
     */
    @JSONField(name = "frame_id")
    private Long frameId;

    /**
     * tunnel frame type
     */
    @JSONField(name = "frame_type")
    private int frameType;

    /**
     * tunnel frame service type
     */
    @JSONField(name = "service_type")
    private String serviceType;

    /**
     * tunnel frame session id
     */
    @JSONField(name = "session_id")
    private String sessionId;

    public String toJSONString() {
        return JSON.toJSONString(this);
    }

    public String toReadableJSONString() {
        Map<String, Object> jsonObject = new HashMap<>();
        jsonObject.put("frame_id", frameId);
        jsonObject.put("frame_type", TunnelFrameType.typeOf(this.frameType));
        jsonObject.put("service_type", this.serviceType);
        jsonObject.put("session_id", sessionId);
        return JSON.toJSONString(jsonObject);
    }

    public Long getFrameId() {
        return frameId;
    }

    public void setFrameId(Long frameId) {
        this.frameId = frameId;
    }

    public int getFrameType() {
        return frameType;
    }

    public void setFrameType(int frameType) {
        this.frameType = frameType;
    }

    public String getServiceType() {
        return serviceType;
    }

    public void setServiceType(String serviceType) {
        this.serviceType = serviceType;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}
