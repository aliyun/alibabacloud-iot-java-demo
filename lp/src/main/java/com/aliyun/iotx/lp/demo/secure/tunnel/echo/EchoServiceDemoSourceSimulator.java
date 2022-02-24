package com.aliyun.iotx.lp.demo.secure.tunnel.echo;

import com.alibaba.fastjson.JSONObject;
import com.aliyun.iotx.lp.demo.secure.tunnel.protocol.source.TunnelSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * the source simulator for echo demo
 */
public class EchoServiceDemoSourceSimulator {
    private static final Logger log = LoggerFactory.getLogger(EchoServiceDemoSourceSimulator.class);

    private final String tunnelId;
    private final TunnelSource tunnelSource;
    private final Map<String, Map<String, JSONObject>> sentMessageOfSession = new ConcurrentHashMap<>();

    public EchoServiceDemoSourceSimulator(String tunnelId, String sourceUri, String token) {
        this.tunnelId = tunnelId;
        EchoServiceDemoReceivedTunnelFrameProcessor receivedTunnelFrameProcessor = new EchoServiceDemoReceivedTunnelFrameProcessor(tunnelId, sentMessageOfSession);
        tunnelSource = new TunnelSource(tunnelId, sourceUri, token, receivedTunnelFrameProcessor);
    }

    public void connect() {
        if (tunnelSource.isConnected()) {
            log.debug("TunnelSource is connected. tunnelId:{}", tunnelId);
        }
        tunnelSource.connect();
    }

    private final ExecutorService executorService = new ThreadPoolExecutor(10, 10, 60, TimeUnit.SECONDS, new ArrayBlockingQueue<>(100), new ThreadFactory() {
        final AtomicInteger count = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "tunnel-session-thread-" + count.getAndIncrement());
        }
    }, new ThreadPoolExecutor.AbortPolicy());


    private final static AtomicInteger MSG_ID = new AtomicInteger(10000);

    public void startEchoSession() {
        executorService.submit(() -> {
            try {
                if (!tunnelSource.isConnected()) {
                    throw new IllegalStateException("tunnelSource should be connected. tunnelId:" + tunnelId);
                }
                String sessionId;
                try {
                    sessionId = tunnelSource.startSession("ECHO");
                } catch (TimeoutException e) {
                    throw new RuntimeException("startEchoSession timeout. tunnelId:" + tunnelId);
                }

                ConcurrentHashMap<String, JSONObject> sentMessage = new ConcurrentHashMap<>();
                sentMessageOfSession.put(sessionId, sentMessage);
                Thread.sleep(3 * 1000);

                for (int i = 0; i < 10; i++) {
                    if (!tunnelSource.isConnected()) {
                        throw new RuntimeException("source closed, data-send interrupted. tunnelId:" + tunnelId);
                    }
                    JSONObject payload = new JSONObject();
                    String key = "payload_key_" + MSG_ID.getAndIncrement();
                    payload.put("key", key);
                    payload.put("value", "payload_value_" + MSG_ID.getAndIncrement());

                    byte[] bytes = payload.toJSONString().getBytes(StandardCharsets.UTF_8);
                    tunnelSource.sendData(sessionId, bytes);

                    sentMessage.put(key, payload);
                    log.debug("complete to send data frame. sessionId:{} key:{}", sessionId, key);
                    Thread.sleep(1000);
                }

                Thread.sleep(10 * 1000);
                if (sentMessage.size() != 0) {
                    log.error("error SourceSimulator sessionId:{} sentMessage.size() != 0", sessionId);
                }

                tunnelSource.closeSession(sessionId);
                Thread.sleep(2 * 1000);
            } catch (InterruptedException | ExecutionException e) {
                log.error("exception " + e.getMessage());
                throw new RuntimeException("");
            }
        });
    }

    public void disconnect() {
        if (tunnelSource.isConnected()) {
            tunnelSource.disconnect();
        }
    }

}
