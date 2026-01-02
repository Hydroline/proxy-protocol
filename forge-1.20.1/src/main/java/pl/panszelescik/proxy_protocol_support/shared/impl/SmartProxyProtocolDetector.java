package pl.panszelescik.proxy_protocol_support.shared.impl;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import pl.panszelescik.proxy_protocol_support.shared.ProxyProtocolSupport;

/**
 * Smart detector that checks the first few bytes to determine if this is a Proxy Protocol connection
 * without blocking direct connections.
 * 
 * @author PanSzelescik
 */
public class SmartProxyProtocolDetector extends ChannelInboundHandlerAdapter {

    private static final byte[] PROXY_V1_PREFIX = "PROXY ".getBytes();
    private static final byte[] PROXY_V2_PREFIX = {0x0D, 0x0A, 0x0D, 0x0A, 0x00, 0x0D, 0x0A, 0x51, 0x55, 0x49, 0x54, 0x0A};
    
    private boolean detected = false;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (ProxyProtocolSupport.debugMode) {
            System.out.println("[代理协议] === 智能检测器已激活 ===");
            System.out.println("[代理协议] 通道: " + ctx.channel().remoteAddress());
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 智能检测器已为以下地址激活: " + ctx.channel().remoteAddress());
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (detected) {
            // Already detected, just pass through
            super.channelRead(ctx, msg);
            return;
        }

        long startTime = System.nanoTime();

        if (!(msg instanceof ByteBuf)) {
            if (ProxyProtocolSupport.debugMode) {
                System.out.println("[代理协议] 非ByteBuf消息，直接传递: " + msg.getClass().getSimpleName());
            }
            super.channelRead(ctx, msg);
            return;
        }

        ByteBuf buffer = (ByteBuf) msg;
        
        if (ProxyProtocolSupport.debugMode) {
            System.out.println("[代理协议] === 智能检测 ===");
            System.out.println("[代理协议] 通道: " + ctx.channel().remoteAddress());
            System.out.println("[代理协议] 缓冲区可读字节数: " + buffer.readableBytes());
        }

        // Need at least 5 bytes to detect "PROXY" or check for Proxy Protocol v2
        if (buffer.readableBytes() < 5) {
            if (ProxyProtocolSupport.debugMode) {
                System.out.println("[代理协议] 字节数不足以进行检测，等待中...");
            }
            super.channelRead(ctx, msg);
            return;
        }

        // Check for Proxy Protocol v1 ("PROXY ")
        boolean isProxyV1 = true;
        for (int i = 0; i < PROXY_V1_PREFIX.length && i < buffer.readableBytes(); i++) {
            if (buffer.getByte(buffer.readerIndex() + i) != PROXY_V1_PREFIX[i]) {
                isProxyV1 = false;
                break;
            }
        }

        // Check for Proxy Protocol v2 (binary signature)
        boolean isProxyV2 = false;
        if (buffer.readableBytes() >= PROXY_V2_PREFIX.length) {
            isProxyV2 = true;
            for (int i = 0; i < PROXY_V2_PREFIX.length; i++) {
                if (buffer.getByte(buffer.readerIndex() + i) != PROXY_V2_PREFIX[i]) {
                    isProxyV2 = false;
                    break;
                }
            }
        }

        detected = true;
        long detectionTime = System.nanoTime() - startTime;
        double detectionMs = detectionTime / 1_000_000.0;

        if (isProxyV1 || isProxyV2) {
            // This is a Proxy Protocol connection!
            if (ProxyProtocolSupport.debugMode) {
                System.out.println("[代理协议] 检测到代理协议! " + (isProxyV1 ? "v1" : "v2") + " (检测耗时: " + String.format("%.2f", detectionMs) + "ms)");
                ProxyProtocolSupport.infoLogger.accept("[代理协议] 从以下地址检测到代理协议 " + (isProxyV1 ? "v1" : "v2") + ": " + ctx.channel().remoteAddress() + " (检测: " + String.format("%.2f", detectionMs) + "ms)");
            }
            
            // Dynamically install HAProxy decoder and handler
            ctx.pipeline()
                .addAfter("smart-detector", "haproxy-decoder", new HAProxyMessageDecoder())
                .addAfter("haproxy-decoder", "haproxy-handler", new ProxyProtocolHandler())
                .remove(this); // Remove this detector
            
            if (ProxyProtocolSupport.debugMode) {
                System.out.println("[代理协议] HAProxy处理器动态安装完成");
                System.out.println("[代理协议] 安装后管道: " + ctx.pipeline().names());
            }
            
            // Re-process the buffer with the new handlers
            super.channelRead(ctx, msg);
        } else {
            // This is a direct connection (normal Minecraft packet)
            if (ProxyProtocolSupport.debugMode) {
                System.out.println("[代理协议] 检测到直连! (检测耗时: " + String.format("%.2f", detectionMs) + "ms)");
                ProxyProtocolSupport.infoLogger.accept("[代理协议] 从以下地址检测到直连: " + ctx.channel().remoteAddress() + " (检测: " + String.format("%.2f", detectionMs) + "ms)");
            }
            
            if (ProxyProtocolSupport.allowDirectConnections) {
                if (ProxyProtocolSupport.debugMode) {
                    System.out.println("[代理协议] 允许直连，移除检测器");
                    ProxyProtocolSupport.infoLogger.accept("[代理协议] 允许直连，直接传递");
                }
                
                // Remove this detector and pass the packet through
                ctx.pipeline().remove(this);
                super.channelRead(ctx, msg);
            } else {
                if (ProxyProtocolSupport.debugMode) {
                    System.out.println("[代理协议] 直连被拒绝 (不允许)");
                }
                ProxyProtocolSupport.warnLogger.accept("[代理协议] 来自以下地址的直连被拒绝: " + ctx.channel().remoteAddress());
                ctx.disconnect();
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (ProxyProtocolSupport.debugMode) {
            System.out.println("[代理协议] === 智能检测器非活跃 ===");
            System.out.println("[代理协议] 通道已断开: " + ctx.channel().remoteAddress());
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (ProxyProtocolSupport.debugMode) {
            System.out.println("[代理协议] === 智能检测器异常 ===");
            System.out.println("[代理协议] 通道: " + ctx.channel().remoteAddress());
            System.out.println("[代理协议] 异常: " + cause.getMessage());
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 智能检测器异常，通道 " + ctx.channel().remoteAddress() + ": " + cause.getMessage());
        }
        super.exceptionCaught(ctx, cause);
    }
} 