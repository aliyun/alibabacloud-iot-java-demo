package com.aliyun.iotx.lp.demo.secure.tunnel.protocol.handler;

import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.dto.TunnelFrame;
import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.dto.TunnelResponse;

/**
 * call back for tunnel response frame
 */
public interface TunnelFrameCallback {
    /**
     * callback method for success
     * @param frame tunnel response frame
     * @param response response
     */
    void onSuccess(TunnelFrame frame, TunnelResponse response);

    /**
     * callback method for failure
     * @param frame tunnel response frame
     * @param response response
     */
    void onFailure(TunnelFrame frame, TunnelResponse response);

}
