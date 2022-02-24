package com.aliyun.iotx.lp.demo.secure.tunnel.protocol.dto;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.common.ChannelAttributeKeyConstant;
import io.netty.buffer.ByteBuf;

import java.nio.charset.StandardCharsets;

/**
 * response payload
 */
public class TunnelResponse {
    public TunnelResponse(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    public TunnelResponse() {
    }

    @JSONField(name = "code")
    private int code;
    @JSONField(name = "msg")
    private String msg;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public static TunnelResponse parse(ByteBuf byteBuf) {
        int responseLength = byteBuf.readableBytes();
        String headerString = byteBuf.toString(byteBuf.readerIndex(), responseLength, StandardCharsets.UTF_8);
        return JSONObject.parseObject(headerString, TunnelResponse.class);
    }

    public String toAbstractInfo() {
        return JSON.toJSONString(this);
    }

    public boolean isSuccess() {
        return code == ChannelAttributeKeyConstant.RESPONSE_CODE_SUCCESS;
    }
}