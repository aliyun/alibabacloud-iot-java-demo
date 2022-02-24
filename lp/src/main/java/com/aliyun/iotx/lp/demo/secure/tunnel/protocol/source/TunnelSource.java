package com.aliyun.iotx.lp.demo.secure.tunnel.protocol.source;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.common.ChannelAttributeKeyConstant;
import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.common.TunnelFrameIdGenerator;
import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.dto.TunnelFrame;
import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.dto.TunnelFrameHeader;
import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.dto.TunnelFrameType;
import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.dto.TunnelResponse;
import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.handler.DeviceSecureTunnelFrameHandler;
import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.handler.ReceivedTunnelFrameProcessor;
import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.handler.TunnelFrameCallback;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

/**
 * convenient api for tunnel source end
 */
public class TunnelSource {
    private static final Logger log = LoggerFactory.getLogger(TunnelSource.class);

    private final String tunnelId;
    private final String connectUrl;
    private final String accessToken;
    private final ReceivedTunnelFrameProcessor receivedTunnelFrameProcessor;

    private Channel channel;
    private EventLoopGroup group;

    public TunnelSource(String tunnelId, String connectUrl, String accessToken, ReceivedTunnelFrameProcessor receivedTunnelFrameProcessor) {
        this.tunnelId = tunnelId;
        this.connectUrl = connectUrl;
        this.accessToken = accessToken;
        this.receivedTunnelFrameProcessor = receivedTunnelFrameProcessor;
    }

    private volatile boolean connected = true;

    public synchronized boolean isConnected() {
        return connected;
    }

    public synchronized void connect() {
        group = new NioEventLoopGroup(1);
        try {
            URI uri = URI.create(connectUrl);

            HttpHeaders httpHeaders = new DefaultHttpHeaders();
            httpHeaders.add("tunnel-access-token", accessToken);

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(group).channel(NioSocketChannel.class).option(ChannelOption.SO_KEEPALIVE, true).handler(
                    new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel channel) throws Exception {
                            ChannelPipeline pipeline = channel.pipeline();
                            SslContext sslCtx = SslContextBuilder.forClient().build();
                            pipeline.addLast(sslCtx.newHandler(channel.alloc()));

                            pipeline.addLast("HttpClientCodec", new HttpClientCodec());
                            pipeline.addLast("HttpObjectAggregator", new HttpObjectAggregator(1024 * 5));
                            //the threshold of websocket frame is 5k because of the threshold of tunnel frame payload length
                            pipeline.addLast("WebSocketClientHandler", new WebSocketClientProtocolHandler(uri, WebSocketVersion.V13, "aliyun.iot.securetunnel-v1.1", true, httpHeaders, 5 * 1024));
                            //handler to handle the received and sent tunnel frame
                            pipeline.addLast("DeviceSecureTunnelFrameHandler", new DeviceSecureTunnelFrameHandler(receivedTunnelFrameProcessor));
                        }
                    });

            int port = uri.getPort();
            if (port == -1) {
                port = 443;
            }

            ChannelFuture connect = bootstrap.connect(uri.getHost(), port);
            Thread.sleep(5 * 1000);
            channel = connect.channel();
            channel.attr(ChannelAttributeKeyConstant.ATTRIBUTE_KEY_TUNNEL_ID).set(tunnelId);

            if (channel.isActive()) {
                log.info("source connect success.");
                connected = true;
            } else {
                log.info("source connect failed.");
                return;
            }

            channel.closeFuture().addListener(f -> {
                log.info("source channel closed.");
                connected = false;
            });

        } catch (Throwable e) {
            log.info("SourceSimulator.connect exception.", e);
            throw new RuntimeException(e);
        }
    }

    public synchronized void disconnect() {
        connected = false;
        if (channel == null) {
            log.error("SourceSimulator close. but channel is null, tunnelId:{}", tunnelId);
        } else {
            channel.close();
        }
        if (group != null && !group.isShutdown()) {
            group.shutdownGracefully(1, 1, TimeUnit.SECONDS);
        }
        log.info("SourceSimulator closed. tunnelId:{}", tunnelId);
    }

    public String startSession(String serviceType) throws ExecutionException, InterruptedException, TimeoutException {
        TunnelFrameHeader header = new TunnelFrameHeader();
        header.setFrameType(TunnelFrameType.SESSION_CREATE.type());
        header.setServiceType(serviceType);
        Long frameId = TunnelFrameIdGenerator.getFrameId();
        header.setFrameId(frameId);

        SessionCreateFuture sessionCreateFuture = new SessionCreateFuture();
        TunnelFrameCallback callback = new TunnelFrameCallback() {
            @Override
            public void onSuccess(TunnelFrame frame, TunnelResponse response) {
                sessionCreateFuture.setSessionId(frame.getFrameHeader().getSessionId());
                log.info("source create session success. sessionId:{}", frame.getFrameHeader().getSessionId());
            }

            @Override
            public void onFailure(TunnelFrame frame, TunnelResponse response) {
                sessionCreateFuture.failed(response == null ? null : response.toString());
                log.error("Source create session failure.response:{}", response);
            }
        };
        receivedTunnelFrameProcessor.addFrameCallback(header.getFrameId(), callback);

        channel.writeAndFlush(TunnelFrame.valueOf(header));

        log.info("SourceSimulator sendSessionCreateFrame header:{}", header.toReadableJSONString());
        return sessionCreateFuture.get(5, TimeUnit.SECONDS);
    }

    public void closeSession(String sessionId) {
        TunnelFrameHeader header = new TunnelFrameHeader();
        header.setFrameType(TunnelFrameType.SESSION_RELEASE.type());
        header.setSessionId(sessionId);
        header.setFrameId(TunnelFrameIdGenerator.getFrameId());

        JSONObject payloadJson = new JSONObject();
        payloadJson.put("code", 0);
        payloadJson.put("msg", "source simulator close session.");
        channel.writeAndFlush(TunnelFrame.valueOf(header, payloadJson.toJSONString().getBytes(StandardCharsets.UTF_8)));
    }

    public void sendData(String sessionId, byte[] payload) {
        if (StringUtils.isBlank(sessionId)) {
            throw new IllegalArgumentException("sessionId should not be blank");
        }
        if (payload == null || payload.length == 0) {
            throw new IllegalArgumentException("payload should not be empty");
        }
        TunnelFrameHeader frameHeader = new TunnelFrameHeader();
        frameHeader.setFrameId(TunnelFrameIdGenerator.getFrameId());
        frameHeader.setFrameType(TunnelFrameType.DATA_TRANSPORT.type());
        frameHeader.setSessionId(sessionId);
        channel.writeAndFlush(TunnelFrame.valueOf(frameHeader, payload));
    }

    private static class SessionCreateFuture implements Future<String> {
        private String sessionId = null;
        private boolean finish;
        private String msg;
        private final CountDownLatch countDownLatch = new CountDownLatch(1);

        public void setSessionId(String sessionId) {
            finish = true;
            this.sessionId = sessionId;
            countDownLatch.countDown();
        }

        public void failed(String msg) {
            finish = true;
            this.msg = msg;
            countDownLatch.countDown();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public boolean isDone() {
            return finish;
        }

        @Override
        public String get() throws InterruptedException, ExecutionException {
            if (isDone()) {
                return sessionId;
            }
            countDownLatch.await();
            if (sessionId != null) {
                return sessionId;
            } else {
                throw new ExecutionException("session creation failed. errorMsg:" + msg, null);
            }
        }

        @Override
        public String get(long timeout, TimeUnit unit)
                throws InterruptedException, ExecutionException, TimeoutException {
            if (isDone()) {
                return sessionId;
            }
            boolean await = countDownLatch.await(timeout, unit);
            if (await) {
                if (sessionId != null) {
                    return sessionId;
                } else {
                    throw new ExecutionException("session creation failed. errorMsg:" + msg, null);
                }
            } else {
                throw new TimeoutException();
            }
        }
    }


}
