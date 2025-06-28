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
        
        try {
            // Install smart detector that will dynamically install HAProxy handlers only when needed
            channel.pipeline()
                    .addAfter("timeout", "smart-detector", new SmartProxyProtocolDetector());
            if (ProxyProtocolSupport.debugMode) {
                System.out.println("[代理协议] 智能代理协议检测器安装成功");
            }
        } catch (Exception e) {
            if (ProxyProtocolSupport.debugMode) {
                System.out.println("[代理协议] 添加智能检测器时发生错误: " + e.getMessage());
                e.printStackTrace();
            }
            throw e;
        }
                
        if (ProxyProtocolSupport.debugMode) {
            System.out.println("[代理协议] 智能检测器安装后的管道: " + channel.pipeline().names());
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 安装后管道: " + channel.pipeline().names());
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 智能代理协议检测器安装成功");
            System.out.println("[代理协议] === 通道初始化完成 ===");
        }
    }
}
