package com.aliyun.iotx.lp.demo.secure.tunnel.source.proxy;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * the main program for source proxy
 */
public class SourceProxyStarter {
    private static final Logger log = LoggerFactory.getLogger(SourceProxyStarter.class);

    private static volatile boolean hasStopped = false;

    public static void main(String[] args) throws JoranException, IOException, InterruptedException {
        load(SourceProxyStarter.class.getClassLoader().getResource("logback-spring.xml").getPath());

        /**
         * please replace the following variables with the tunnel connection info
         */
        String tunnelId = "";
        String sourceUri = "";
        String sourceToken = "";

        /**
         * please replace the portOfService variables with the service type supported by device
         */
        Map<String, Integer> portOfService = new HashMap<>();
        portOfService.put("CUSTOM_SSH", 6422);

        if (StringUtils.isBlank(tunnelId) || StringUtils.isBlank(sourceUri) || StringUtils.isBlank(sourceToken)) {
            throw new IllegalArgumentException("tunnelId, sourceUri and sourceToken should not be blank. please crate device tunnel and assign the corresponding value");
        }

        DeviceTunnelSourceProxy sourceProxy = new DeviceTunnelSourceProxy(tunnelId, sourceUri, sourceToken, portOfService);
        sourceProxy.start();

        startAwaitThread();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("SourceProxyStarter shutdown, so to stop the source simulator.");
            sourceProxy.stop();
            hasStopped = true;
        }));
    }

    private static void startAwaitThread() {
        Thread thread = new Thread(() -> {
            while (!hasStopped) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    // continue and check the flag
                }
            }
        }, "SourceProxyStarter-awaitStopThread");
        thread.setDaemon(false);
        thread.start();
    }


    public static void load(String externalConfigFileLocation) throws IOException, JoranException {
        LoggerContext lc = (LoggerContext) LoggerFactory.getILoggerFactory();

        File externalConfigFile = new File(externalConfigFileLocation);
        if (!externalConfigFile.exists()) {
            throw new IOException("Logback External Config File Parameter does not reference a file that exists");
        } else {
            if (!externalConfigFile.isFile()) {
                throw new IOException("Logback External Config File Parameter exists, but does not reference a file");
            } else {
                if (!externalConfigFile.canRead()) {
                    throw new IOException("Logback External Config File exists and is a file, but cannot be read.");
                } else {
                    JoranConfigurator configurator = new JoranConfigurator();
                    configurator.setContext(lc);
                    lc.reset();
                    configurator.doConfigure(externalConfigFileLocation);
                    StatusPrinter.printInCaseOfErrorsOrWarnings(lc);
                }
            }
        }
    }
}
