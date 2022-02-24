package com.aliyun.iotx.lp.demo.secure.tunnel.protocol.dto;

/**
 * supported tunnel frame type
 */
public enum TunnelFrameType {
    /**
     * the frame type for response
     */
    COMMON_RESPONSE(1),
    /**
     * the frame type for session creation
     */
    SESSION_CREATE(2),
    /**
     * the frame type for session release
     */
    SESSION_RELEASE(3),
    /**
     * the frame type for data transfer
     */
    DATA_TRANSPORT(4);

    private final int type;

    TunnelFrameType(int type) {
        this.type = type;
    }

    public static TunnelFrameType typeOf(int type) {
        TunnelFrameType messageType = typeOfOrNull(type);

        if (messageType == null) {
            throw new UnsupportedOperationException("unsupported message type='" + type + "'");
        }

        return messageType;
    }

    public static TunnelFrameType typeOfOrNull(int type) {
        for (TunnelFrameType value : values()) {
            if (value.type == type) {
                return value;
            }
        }
        return null;
    }

    public int type() {
        return type;
    }
}