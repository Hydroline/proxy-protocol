package com.hydroline.proxy.protocol.shared;

import com.hydroline.proxy.protocol.shared.config.CIDRMatcher;
import com.hydroline.proxy.protocol.shared.config.Config;
import com.hydroline.proxy.protocol.shared.config.TCPShieldIntegration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple class for getting settings
 *
 * @author PanSzelescik
 */
public class ProxyProtocolSupport {

    public static final String MODID = "proxy_protocol";
    public static final String CONFIG_FILE_NAME = "proxy-protocol.json";
    public static final String LOG_PREFIX = "[Proxy Protocol] ";

    public static Consumer<String> infoLogger = message -> System.out.println(LOG_PREFIX + message);
    public static Consumer<String> warnLogger = message -> System.out.println(LOG_PREFIX + message);
    public static Consumer<String> errorLogger = message -> System.out.println(LOG_PREFIX + message);
    public static BiConsumer<String, Exception> exceptionLogger = (message, exception) -> {
        errorLogger.accept(message);
        exception.printStackTrace();
    };

    public static boolean enableProxyProtocol = false;
    public static boolean allowDirectConnections = true;
    public static boolean whitelistMode = false;
    public static boolean debugMode = false;
    public static Collection<CIDRMatcher> whitelistedIPs = new ArrayList<>();

    public static void initialize(Config config) throws IOException {
        loggerInitialize();
        ProxyProtocolSupport.debugMode = config.debugMode;

        logDebug("initialize() called");
        logDebug("Configuration: enable=" + config.enableProxyProtocol
                + ", direct=" + config.allowDirectConnections
                + ", whitelist=" + config.whitelistMode
                + ", debug=" + config.debugMode
                + ", whitelistIPs=" + config.whitelistedIPs.size()
                + ", tcpShield=" + config.whitelistTCPShieldServers);

        if (!config.enableProxyProtocol) {
            infoLogger.accept("Proxy Protocol is disabled by configuration.");
            return;
        }

        infoLogger.accept(String.format("Proxy Protocol enabled (directConnections=%s, whitelistMode=%s, debugMode=%s)",
                config.allowDirectConnections,
                config.whitelistMode,
                config.debugMode));

        enableProxyProtocol = config.enableProxyProtocol;
        allowDirectConnections = config.allowDirectConnections;
        whitelistMode = config.whitelistMode;

        logDebug("Processing whitelist entries.");
        whitelistedIPs = config.whitelistedIPs
                .stream()
                .map(CIDRMatcher::new)
                .collect(Collectors.toSet());

        infoLogger.accept("Loaded " + whitelistedIPs.size() + " whitelist entries.");
        if (config.whitelistTCPShieldServers) {
            infoLogger.accept("TCPShield integration enabled, merging official whitelist.");
            whitelistedIPs.addAll(Stream
                    .concat(whitelistedIPs.stream(), TCPShieldIntegration.getWhitelistedIPs().stream())
                    .collect(Collectors.toSet()));
        } else {
            logDebug("TCPShield integration disabled.");
        }

        infoLogger.accept("Final whitelist size: " + whitelistedIPs.size());
    }

    public static void logDebug(String message) {
        if (debugMode) {
            infoLogger.accept(message);
        }
    }

    private static void loggerInitialize() {
        if (hasSlf4jBinding()) {
            try {
                org.slf4j.Logger slf4j = org.slf4j.LoggerFactory.getLogger(MODID);
                infoLogger = message -> slf4j.info(LOG_PREFIX + message);
                warnLogger = message -> slf4j.warn(LOG_PREFIX + message);
                errorLogger = message -> slf4j.error(LOG_PREFIX + message);
                exceptionLogger = (message, exception) -> slf4j.error(LOG_PREFIX + message, exception);
                return;
            } catch (Throwable ignored) {
                // fall through to log4j fallback below
            }
        }

        try {
            org.apache.logging.log4j.Logger log4j = org.apache.logging.log4j.LogManager.getLogger(MODID);
            infoLogger = message -> log4j.info(LOG_PREFIX + message);
            warnLogger = message -> log4j.warn(LOG_PREFIX + message);
            errorLogger = message -> log4j.error(LOG_PREFIX + message);
            exceptionLogger = (message, exception) -> log4j.error(LOG_PREFIX + message, exception);
            return;
        } catch (Throwable ignored2) {
            infoLogger = message -> System.out.println(LOG_PREFIX + message);
            warnLogger = message -> System.out.println(LOG_PREFIX + message);
            errorLogger = message -> System.out.println(LOG_PREFIX + message);
            exceptionLogger = (message, exception) -> {
                System.out.println(LOG_PREFIX + message);
                exception.printStackTrace();
            };
        }
    }

    private static boolean hasSlf4jBinding() {
        try {
            Class.forName("org.slf4j.impl.StaticLoggerBinder");
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }
}