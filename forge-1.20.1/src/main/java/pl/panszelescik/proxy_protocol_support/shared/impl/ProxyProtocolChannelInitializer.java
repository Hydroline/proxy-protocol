package pl.panszelescik.proxy_protocol_support.shared.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import pl.panszelescik.proxy_protocol_support.shared.ProxyProtocolSupport;

/**
 * Initializes HAProxyMessageDecoder and ProxyProtocolHandler
 *
 * @author PanSzelescik
 * @see io.netty.handler.codec.haproxy.HAProxyMessageDecoder
 * @see ProxyProtocolHandler
 */
public class ProxyProtocolChannelInitializer extends ChannelInitializer {

    private final IChannelInitializer channelInitializer;

    public ProxyProtocolChannelInitializer(IChannelInitializer channelInitializer) {
        this.channelInitializer = channelInitializer;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        if (ProxyProtocolSupport.debugMode) {
            System.out.println("[代理协议] === 通道初始化开始 ===");
            System.out.println("[代理协议] 通道: " + channel.remoteAddress());
            System.out.println("[代理协议] 当前 enableProxyProtocol: " + ProxyProtocolSupport.enableProxyProtocol);
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 初始化通道: " + channel.remoteAddress());
        }
        
        this.channelInitializer.invokeInitChannel(channel);
        
        if (ProxyProtocolSupport.debugMode) {
            System.out.println("[代理协议] 基础通道初始化完成");
        }

        if (!ProxyProtocolSupport.enableProxyProtocol) {
            if (ProxyProtocolSupport.debugMode) {
                if (ProxyProtocolSupport.allowDirectConnections) {
                    ProxyProtocolSupport.infoLogger.accept("[代理协议] 代理协议已禁用，允许直连");
                    System.out.println("[代理协议] 允许直连 (代理协议已禁用)");
                } else {
                    ProxyProtocolSupport.infoLogger.accept("[代理协议] 代理协议已禁用且直连已禁用");
                    System.out.println("[代理协议] 代理和直连均已禁用");
                }
            }
            return;
        }

        if (ProxyProtocolSupport.debugMode) {
            if (ProxyProtocolSupport.allowDirectConnections) {
                if (ProxyProtocolSupport.whitelistMode) {
                    ProxyProtocolSupport.infoLogger.accept("[代理协议] 混合+白名单模式: 带IP限制的智能代理协议");
                    System.out.println("[代理协议] 混合+白名单模式");
                } else {
                    ProxyProtocolSupport.infoLogger.accept("[代理协议] 混合+自动检测模式: 带自动检测的智能代理协议 (推荐)");
                    System.out.println("[代理协议] 混合+自动检测模式 (无需白名单!)");
                }
            } else {
                if (ProxyProtocolSupport.whitelistMode) {
                    ProxyProtocolSupport.infoLogger.accept("[代理协议] 纯代理+白名单模式: 带IP限制的严格代理协议");
                    System.out.println("[代理协议] 纯代理+白名单模式");
                } else {
                    ProxyProtocolSupport.infoLogger.accept("[代理协议] 纯代理+自动检测模式: 接受任何代理连接");
                    System.out.println("[代理协议] 纯代理+自动检测模式");
                }
            }

            System.out.println("[代理协议] 代理协议已启用，正在安装智能检测器...");
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 代理协议已启用，正在安装智能检测器...");
            
            System.out.println("[代理协议] 智能检测器安装前的管道: " + channel.pipeline().names());
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 安装前管道: " + channel.pipeline().names());
        }
        
        // Install smart detector that will dynamically install HAProxy handlers only when needed.
        // NOTE: Pipeline handler names differ across Minecraft/Forge versions; be defensive.
        installSmartDetector(channel);
                
        if (ProxyProtocolSupport.debugMode) {
            System.out.println("[代理协议] 智能检测器安装后的管道: " + channel.pipeline().names());
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 安装后管道: " + channel.pipeline().names());
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 智能代理协议检测器安装成功");
            System.out.println("[代理协议] === 通道初始化完成 ===");
        }
    }

    private static void installSmartDetector(Channel channel) {
        final var pipeline = channel.pipeline();

        // Prefer to place detector early (before packet decoding/handling), but after timeout if present.
        final String[] preferredAnchors = new String[] {
                "timeout",
                "read_timeout",
                "legacy_query",
                "splitter",
                "decoder",
                "packet_handler"
        };

        String anchor = null;
        for (String name : preferredAnchors) {
            if (pipeline.get(name) != null) {
                anchor = name;
                break;
            }
        }

        if (ProxyProtocolSupport.debugMode) {
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 尝试安装智能检测器，当前管道: " + pipeline.names());
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 选定锚点: " + (anchor == null ? "<none>" : anchor));
        }

        try {
            if (anchor == null) {
                pipeline.addFirst("smart-detector", new SmartProxyProtocolDetector());
            } else if ("packet_handler".equals(anchor)) {
                pipeline.addBefore(anchor, "smart-detector", new SmartProxyProtocolDetector());
            } else {
                pipeline.addAfter(anchor, "smart-detector", new SmartProxyProtocolDetector());
            }
        } catch (Exception e) {
            // Last-resort: do not break the connection if insertion fails.
            // Falling back to addFirst is safer than throwing and killing handshake.
            if (ProxyProtocolSupport.debugMode) {
                ProxyProtocolSupport.exceptionLogger.accept("[代理协议] 安装智能检测器失败，尝试回退到 addFirst", (e instanceof Exception) ? (Exception) e : new RuntimeException(e));
            }
            try {
                if (pipeline.get("smart-detector") == null) {
                    pipeline.addFirst("smart-detector", new SmartProxyProtocolDetector());
                }
            } catch (Exception ignored) {
                // If we still can't install, keep server functional (no proxy protocol support).
                if (ProxyProtocolSupport.debugMode) {
                    ProxyProtocolSupport.warnLogger.accept("[代理协议] 回退安装也失败，已跳过智能检测器安装以保证连接可用");
                }
            }
        }
    }
}
