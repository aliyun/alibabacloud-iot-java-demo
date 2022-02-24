package com.aliyun.iotx.lp.demo.secure.tunnel.protocol.common;

import io.netty.util.AttributeKey;

/**
 * channel attribute key constant
 */
public class ChannelAttributeKeyConstant {
    public static final AttributeKey<String> ATTRIBUTE_KEY_TUNNEL_ID = AttributeKey.valueOf("tunnel-id");
    public static final int RESPONSE_CODE_SUCCESS = 0;
}
