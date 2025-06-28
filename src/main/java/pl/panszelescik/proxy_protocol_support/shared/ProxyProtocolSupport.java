package pl.panszelescik.proxy_protocol_support.shared;

import pl.panszelescik.proxy_protocol_support.shared.config.CIDRMatcher;
import pl.panszelescik.proxy_protocol_support.shared.config.Config;
import pl.panszelescik.proxy_protocol_support.shared.config.TCPShieldIntegration;

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

    public static final String MODID = "proxy_protocol_support";

    public static Consumer<String> infoLogger = System.out::println;
    public static Consumer<String> warnLogger = System.out::println;
    public static Consumer<String> errorLogger = System.out::println;
    public static BiConsumer<String, Exception> exceptionLogger = (s, e) -> {
        errorLogger.accept(s);
        e.printStackTrace();
    };

    public static boolean enableProxyProtocol = false;
    public static boolean allowDirectConnections = true;
    public static boolean whitelistMode = false;
    public static boolean debugMode = false;
    public static Collection<CIDRMatcher> whitelistedIPs = new ArrayList<>();

    public static void initialize(Config config) throws IOException {
        if (config.debugMode) {
            System.out.println("=== [代理协议] 初始化方法被调用 ===");
        }
        loggerInitialize();
        
        ProxyProtocolSupport.debugMode = config.debugMode;

        if (config.debugMode) {
            System.out.println("=== [代理协议] 配置调试信息 ===");
            System.out.println("[代理协议] config.enableProxyProtocol = " + config.enableProxyProtocol);
            System.out.println("[代理协议] config.allowDirectConnections = " + config.allowDirectConnections);
            System.out.println("[代理协议] config.whitelistMode = " + config.whitelistMode);
            System.out.println("[代理协议] config.debugMode = " + config.debugMode);
            System.out.println("[代理协议] config.whitelistedIPs.size() = " + config.whitelistedIPs.size());
            System.out.println("[代理协议] config.whitelistedIPs = " + config.whitelistedIPs);
            System.out.println("[代理协议] config.whitelistTCPShieldServers = " + config.whitelistTCPShieldServers);
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 配置调试 - 启用代理协议: " + config.enableProxyProtocol);
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 配置调试 - 允许直连: " + config.allowDirectConnections);
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 配置调试 - 白名单模式: " + config.whitelistMode);
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 配置调试 - 调试模式: " + config.debugMode);
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 配置调试 - 白名单IP: " + config.whitelistedIPs);
            
            System.out.println("[代理协议] 布尔检查 - config.enableProxyProtocol: " + config.enableProxyProtocol);
            System.out.println("[代理协议] 布尔检查 - !config.enableProxyProtocol: " + (!config.enableProxyProtocol));
            System.out.println("[代理协议] 布尔检查 - 进入if语句: " + (!config.enableProxyProtocol));
        }

        if (!config.enableProxyProtocol) {
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 代理协议在配置中已禁用");
            if (config.debugMode) {
                System.out.println("=== [代理协议] 代理协议已禁用 ===");
            }
            return;
        }
        
        ProxyProtocolSupport.infoLogger.accept("[代理协议] 代理协议已启用 - 混合模式: " + config.allowDirectConnections + 
                                                ", 白名单模式: " + config.whitelistMode + 
                                                ", 调试模式: " + config.debugMode);

        if (config.debugMode) {
            System.out.println("=== [代理协议] 代理协议已启用 ===");
            System.out.println("[代理协议] 设置静态 enableProxyProtocol = " + config.enableProxyProtocol);
            System.out.println("[代理协议] 设置静态 allowDirectConnections = " + config.allowDirectConnections);
            System.out.println("[代理协议] 设置静态 whitelistMode = " + config.whitelistMode);
            System.out.println("[代理协议] 设置静态 debugMode = " + config.debugMode);
        }
        
        ProxyProtocolSupport.enableProxyProtocol = config.enableProxyProtocol;
        ProxyProtocolSupport.allowDirectConnections = config.allowDirectConnections;
        ProxyProtocolSupport.whitelistMode = config.whitelistMode;
        
        if (config.debugMode) {
            System.out.println("[代理协议] 正在处理白名单IP...");
        }
        ProxyProtocolSupport.whitelistedIPs = config.whitelistedIPs
                .stream()
                .map(CIDRMatcher::new)
                .collect(Collectors.toSet());
        
        if (config.debugMode) {
            System.out.println("[代理协议] 已处理 " + ProxyProtocolSupport.whitelistedIPs.size() + " 个白名单IP");
        }

        if (config.whitelistTCPShieldServers) {
            if (config.debugMode) {
                System.out.println("[代理协议] 正在处理TCPShield集成...");
            }
            ProxyProtocolSupport.infoLogger.accept("[代理协议] TCPShield集成已启用");
            whitelistedIPs.addAll(Stream
                    .concat(whitelistedIPs.stream(), TCPShieldIntegration.getWhitelistedIPs().stream())
                    .collect(Collectors.toSet()));
            if (config.debugMode) {
                System.out.println("[代理协议] TCPShield集成完成");
            }
        } else if (config.debugMode) {
            System.out.println("[代理协议] TCPShield集成已禁用");
        }

        ProxyProtocolSupport.infoLogger.accept("[代理协议] 配置加载成功，包含 " + ProxyProtocolSupport.whitelistedIPs.size() + " 个白名单IP");
        
        if (config.debugMode) {
            System.out.println("[代理协议] 最终白名单IP数量: " + ProxyProtocolSupport.whitelistedIPs.size());
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 使用 " + ProxyProtocolSupport.whitelistedIPs.size() + " 个白名单IP: " + ProxyProtocolSupport.whitelistedIPs);
            System.out.println("[代理协议] 代理协议配置完成！");
            System.out.println("=== [代理协议] 初始化方法结束 ===");
        }
    }

    private static void loggerInitialize() {
        try {
            org.slf4j.Logger slf4j = org.slf4j.LoggerFactory.getLogger(MODID);
            infoLogger = slf4j::info;
            warnLogger = slf4j::warn;
            errorLogger = slf4j::error;
            exceptionLogger = slf4j::error;
        } catch (Throwable ignored) {
            try {
                org.apache.logging.log4j.Logger log4j = org.apache.logging.log4j.LogManager.getLogger(MODID);
                infoLogger = log4j::info;
                warnLogger = log4j::warn;
                errorLogger = log4j::error;
                exceptionLogger = log4j::error;
            } catch (Throwable ignored2) {
                infoLogger = System.out::println;
                warnLogger = System.out::println;
                errorLogger = System.out::println;
                exceptionLogger = (s, e) -> {
                    errorLogger.accept(s);
                    e.printStackTrace();
                };
            }
        }
    }
}
