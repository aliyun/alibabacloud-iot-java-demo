package com.aliyun.iotx.lp.demo.secure.tunnel.protocol.dto;

import com.alibaba.fastjson.JSONObject;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.DefaultByteBufHolder;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * tunnel frame
 */
public class TunnelFrame extends DefaultByteBufHolder {
    /**
     * header
     */
    private final TunnelFrameHeader frameHeader;

    /**
     * byte array of content
     * only assign if necessary
     */
    private volatile byte[] payloadBytes;

    private TunnelFrame(TunnelFrameHeader frameHeader, ByteBuf payload) {
        super(payload);
        Objects.requireNonNull(frameHeader, "messageHeader");
        this.frameHeader = frameHeader;
    }

    public static TunnelFrame valueOf(TunnelFrameHeader frameHeader, byte[] payload) {
        Objects.requireNonNull(payload, "payload");
        return valueOf(frameHeader, Unpooled.wrappedBuffer(payload));
    }

    public static TunnelFrame valueOf(TunnelFrameHeader frameHeader) {
        return valueOf(frameHeader, Unpooled.buffer(0));
    }

    public static TunnelFrame valueOf(TunnelFrameHeader frameHeader, ByteBuf payload) {
        return new TunnelFrame(frameHeader, payload);
    }

    public static TunnelFrame parse(ByteBuf byteBuf) {
        int headerLength = byteBuf.readUnsignedShort();

        byte[] headerBytes = new byte[headerLength];
        byteBuf.readBytes(headerBytes);

        String headerString = new String(headerBytes, StandardCharsets.UTF_8);
        TunnelFrameHeader frameHeader = JSONObject.parseObject(headerString, TunnelFrameHeader.class);

        return valueOf(frameHeader, byteBuf);
    }

    @Override
    public TunnelFrame retain() {
        super.retain();
        return this;
    }

    public TunnelFrameHeader getFrameHeader() {
        return frameHeader;
    }

    public byte[] getPayloadBytes() {
        if (payloadBytes != null) {
            return payloadBytes;
        }
        synchronized (this) {
            if (payloadBytes != null) {
                return payloadBytes;
            }

            payloadBytes = ByteBufUtil.getBytes(content());
        }
        return payloadBytes;
    }

    private static final int MAX_HEADER_LENGTH = 2048;

    public ByteBuf frameToByteBuf() {
        byte[] headerBytes = this.frameHeader.toJSONString().getBytes(StandardCharsets.UTF_8);

        if (headerBytes.length > MAX_HEADER_LENGTH) {
            throw new RuntimeException("header length has exceeded 2048");
        }

        ByteBuf headerLengthByteBuf = Unpooled.buffer(2);
        headerLengthByteBuf.writeShort(headerBytes.length);

        return Unpooled.wrappedBuffer(headerLengthByteBuf.array(), headerBytes, this.getPayloadBytes());
    }

    @Override
    public String toString() {
        return frameHeader.toJSONString() + " " + "payload length:" + getPayloadBytes().length;
    }
}
