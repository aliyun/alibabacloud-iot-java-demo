package com.aliyun.iotx.lp.demo.secure.tunnel.source.proxy;

import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.source.TunnelSource;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.AttributeKey;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * the proxy to access device service using device secure tunnel
 */
public class DeviceTunnelSourceProxy {

    private static final Logger log = LoggerFactory.getLogger(DeviceTunnelSourceProxy.class);

    private final String tunnelId;
    private final TunnelSource tunnelSource;
    private final Map<String, Channel> channelOfSession = new ConcurrentHashMap<>();
    private Map<String, Integer> portOfService;
    private final static AttributeKey<String> CHANNEL_ATT_SESSION_ID = AttributeKey.valueOf("attribute_key_session_id");

    public DeviceTunnelSourceProxy(String tunnelId, String sourceUri, String token, Map<String, Integer> portOfService) {
        this.tunnelId = tunnelId;
        this.portOfService = portOfService;
        if (portOfService == null || portOfService.size() == 0) {
            throw new IllegalArgumentException("portOfService should not be null or empty.");
        }
        if (portOfService.size() > 10) {
            throw new IllegalArgumentException("portOfService can at most 10 services.");
        }
        SourceProxyFrameProcessor receivedTunnelFrameProcessor = new SourceProxyFrameProcessor(tunnelId, channelOfSession);
        tunnelSource = new TunnelSource(tunnelId, sourceUri, token, receivedTunnelFrameProcessor);
    }

    public synchronized void start() {
        tunnelSource.connect();
        if (!tunnelSource.isConnected()) {
            log.error("DeviceTunnelSourceProxy start failed. tunnelSource is disconnected");
        }
        log.debug("DeviceTunnelSourceProxy tunnel connected. tunnelId:{}", tunnelId);
        this.startProxyListener();
        log.debug("DeviceTunnelSourceProxy started. tunnelId:{}", tunnelId);
    }

    private void startProxyListener() {
        try {
            log.info("DeviceTunnelSourceProxy listener start");
            EventLoopGroup bossGroup = new NioEventLoopGroup(1);
            EventLoopGroup workerGroup = new NioEventLoopGroup();

            portOfService.forEach((serviceType, proxyPort) -> {
                ServerBootstrap bootstrap = new ServerBootstrap();
                bootstrap.group(bossGroup, workerGroup)
                        .channel(NioServerSocketChannel.class)
                        .childHandler(new ChannelInitializer<SocketChannel>() {
                            @Override
                            protected void initChannel(SocketChannel ch) throws Exception {
                                ChannelPipeline pipeline = ch.pipeline();
                                String sessionId = tunnelSource.startSession(serviceType);
                                if (StringUtils.isBlank(sessionId)) {
                                    log.error("sessionId should not be blank, tunnelId:{} serviceType:{}", tunnelId, serviceType);
                                    throw new RuntimeException("sessionId should not be blank");
                                }
                                channelOfSession.put(sessionId, ch);
                                ch.attr(CHANNEL_ATT_SESSION_ID).set(sessionId);
                                pipeline.addLast(new IdleStateHandler(0, 0, 1800, TimeUnit.SECONDS));
                                pipeline.addLast(new SourceProxyChannelHandler(tunnelId, tunnelSource));
                            }
                        })
                        .option(ChannelOption.SO_BACKLOG, 1024)
                        .option(ChannelOption.SO_RCVBUF, 64 * 1024)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childOption(ChannelOption.TCP_NODELAY, true)
                        .childOption(ChannelOption.AUTO_CLOSE, true)
                        .childOption(ChannelOption.ALLOW_HALF_CLOSURE, false)
                        .childOption(ChannelOption.SO_REUSEADDR, true);

                try {
                    ChannelFuture syncFuture = bootstrap.bind(proxyPort).sync();
                    Channel channel = syncFuture.channel();
                    log.info("DeviceTunnelSourceProxy listener channelId:{}", channel.id());
                } catch (InterruptedException e) {
                    log.error("DeviceTunnelSourceProxy bind port:{} interrupted.", proxyPort);
                    throw new RuntimeException("DeviceTunnelSourceProxy bind port:" + proxyPort + " interrupted.");
                }
            });

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                log.info("close DeviceTunnelSourceProxy listener, serverClass={}", getClass().getName());
                if (!bossGroup.isShutdown()) {
                    bossGroup.shutdownGracefully();
                }
                if (!workerGroup.isShutdown()) {
                    workerGroup.shutdownGracefully();
                }
            }));
            log.info("DeviceTunnelSourceProxy listener started");
        } catch (Throwable e) {
            log.error("DeviceTunnelSourceProxy listener start exception.", e);
            throw new RuntimeException(e);
        }
    }

    public synchronized void stop() {
        if (tunnelSource.isConnected()) {
            tunnelSource.disconnect();
        }
        log.debug("DeviceTunnelSourceProxy stopped. tunnelId:{}", tunnelId);
    }
}
