package com.aliyun.iotx.lp.demo.secure.tunnel.echo;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

/**
 * this is the example for echo service using the device tunnel of aliyun iot
 */
public class EchoServiceDemoTunnelSourceTest {
    public static void main(String[] args) throws InterruptedException, JoranException, IOException {
        load(EchoServiceDemoTunnelSourceTest.class.getClassLoader().getResource("logback-spring.xml").getPath());

        String tunnelId = "";
        String sourceUri = "";
        String sourceToken = "";

        if (StringUtils.isBlank(tunnelId) || StringUtils.isBlank(sourceUri) || StringUtils.isBlank(sourceToken)) {
            throw new IllegalArgumentException("tunnelId, sourceUri and sourceToken should not be blank. please crate device tunnel and assign the corresponding value");
        }

        EchoServiceDemoSourceSimulator sourceSimulator = new EchoServiceDemoSourceSimulator(tunnelId, sourceUri, sourceToken);
        sourceSimulator.connect();
        for (int i = 0; i < 2; i++) {
            sourceSimulator.startEchoSession();
        }
        Thread.sleep(20 * 1000);
        sourceSimulator.disconnect();
        Thread.sleep(3 * 1000);
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
