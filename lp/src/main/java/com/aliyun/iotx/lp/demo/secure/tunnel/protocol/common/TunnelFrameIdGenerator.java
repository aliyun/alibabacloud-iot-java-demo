package com.aliyun.iotx.lp.demo.secure.tunnel.protocol.common;

import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

/**
 * tunnel frame id generator
 */
public class TunnelFrameIdGenerator {
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final AtomicLong FRAME_ID = new AtomicLong(1000 + RANDOM.nextInt(10000));

    public static long getFrameId() {
        long value = FRAME_ID.getAndIncrement();
        if (Long.MAX_VALUE - value < 10000) {
            FRAME_ID.set(1000 + RANDOM.nextInt(10000));
        }
        return value;
    }

}
