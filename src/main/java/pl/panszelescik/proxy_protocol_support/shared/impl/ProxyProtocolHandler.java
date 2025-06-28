package pl.panszelescik.proxy_protocol_support.shared.impl;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.haproxy.HAProxyCommand;
import io.netty.handler.codec.haproxy.HAProxyMessage;
import net.minecraft.network.Connection;
import pl.panszelescik.proxy_protocol_support.shared.ProxyProtocolSupport;
import pl.panszelescik.proxy_protocol_support.shared.config.CIDRMatcher;
import pl.panszelescik.proxy_protocol_support.shared.mixin.ProxyProtocolAddressSetter;

import java.net.InetSocketAddress;
import java.net.SocketAddress;

/**
 * Reads HAProxyMessage to set valid Player IP
 *
 * @author PanSzelescik
 * @see io.netty.handler.codec.haproxy.HAProxyMessage
 */
public class ProxyProtocolHandler extends ChannelInboundHandlerAdapter {

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        if (ProxyProtocolSupport.debugMode) {
            System.out.println("[代理协议] === 处理器已激活 ===");
            System.out.println("[代理协议] 通道: " + ctx.channel().remoteAddress());
            System.out.println("[代理协议] 管道: " + ctx.channel().pipeline().names());
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 处理器已为通道激活: " + ctx.channel().remoteAddress());
        }
        super.channelActive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (ProxyProtocolSupport.debugMode) {
            System.out.println("[代理协议] === 通道读取 ===");
            System.out.println("[代理协议] 通道: " + ctx.channel().remoteAddress());
            System.out.println("[代理协议] 消息类型: " + msg.getClass().getSimpleName());
            System.out.println("[代理协议] 消息: " + msg);
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 收到消息类型: " + msg.getClass().getSimpleName());
            if (msg instanceof HAProxyMessage) {
                System.out.println("[代理协议] 检测到HAProxyMessage!");
            } else {
                System.out.println("[代理协议] 不是HAProxyMessage，类型: " + msg.getClass().getName());
            }
        }
        if (msg instanceof HAProxyMessage) {
            if (ProxyProtocolSupport.debugMode) {
                System.out.println("[代理协议] 正在处理HAProxyMessage...");
                ProxyProtocolSupport.infoLogger.accept("[代理协议] 正在处理HAProxyMessage");
            }
            try {
                HAProxyMessage message = ((HAProxyMessage) msg);
                if (ProxyProtocolSupport.debugMode) {
                    System.out.println("[代理协议] HAProxy命令: " + message.command());
                    System.out.println("[代理协议] HAProxy版本: " + message.protocolVersion());
                    ProxyProtocolSupport.infoLogger.accept("[代理协议] HAProxy命令: " + message.command());
                    ProxyProtocolSupport.infoLogger.accept("[代理协议] HAProxy版本: " + message.protocolVersion());
                }
                
                if (message.command() == HAProxyCommand.PROXY) {
                    if (ProxyProtocolSupport.debugMode) {
                        ProxyProtocolSupport.infoLogger.accept("[代理协议] 正在处理PROXY命令");
                    }
                    final String realAddress = message.sourceAddress();
                    final int realPort = message.sourcePort();
                    final String destAddress = message.destinationAddress();
                    final int destPort = message.destinationPort();

                    if (ProxyProtocolSupport.debugMode) {
                        ProxyProtocolSupport.infoLogger.accept("[代理协议] 真实客户端: " + realAddress + ":" + realPort);
                        ProxyProtocolSupport.infoLogger.accept("[代理协议] 目标地址: " + destAddress + ":" + destPort);
                    }

                    final InetSocketAddress socketAddr = new InetSocketAddress(realAddress, realPort);
                    
                    if (ProxyProtocolSupport.debugMode) {
                        ProxyProtocolSupport.infoLogger.accept("[代理协议] 在管道中查找NetworkManager...");
                    }
                    Connection connection = ((Connection) ctx.channel().pipeline().get("packet_handler"));
                    if (connection == null) {
                        ProxyProtocolSupport.warnLogger.accept("[代理协议] 错误: 在管道中未找到NetworkManager!");
                        if (ProxyProtocolSupport.debugMode) {
                            ProxyProtocolSupport.infoLogger.accept("[代理协议] 可用处理器: " + ctx.channel().pipeline().names());
                        }
                        return;
                    }
                    if (ProxyProtocolSupport.debugMode) {
                        ProxyProtocolSupport.infoLogger.accept("[代理协议] NetworkManager找到成功");
                    }
                    
                    SocketAddress proxyAddress = connection.getRemoteAddress();
                    if (ProxyProtocolSupport.debugMode) {
                        ProxyProtocolSupport.infoLogger.accept("[代理协议] 代理地址: " + proxyAddress);
                    }
                    
                    if (ProxyProtocolSupport.whitelistMode && !ProxyProtocolSupport.whitelistedIPs.isEmpty()) {
                        if (ProxyProtocolSupport.debugMode) {
                            ProxyProtocolSupport.infoLogger.accept("[代理协议] 白名单模式: 检查IP白名单 (" + ProxyProtocolSupport.whitelistedIPs.size() + " 条目)");
                        }
                        if (proxyAddress instanceof InetSocketAddress) {
                            InetSocketAddress proxySocketAddress = ((InetSocketAddress) proxyAddress);
                            if (ProxyProtocolSupport.debugMode) {
                                ProxyProtocolSupport.infoLogger.accept("[代理协议] 需检查的代理IP: " + proxySocketAddress.getAddress().getHostAddress());
                            }
                            boolean isWhitelistedIP = false;

                            int i = 0;
                            for (CIDRMatcher matcher : ProxyProtocolSupport.whitelistedIPs) {
                                if (ProxyProtocolSupport.debugMode) {
                                    ProxyProtocolSupport.infoLogger.accept("[代理协议] 对比白名单[" + i + "]: " + matcher.toString());
                                }
                                if (matcher.matches(proxySocketAddress.getAddress())) {
                                    if (ProxyProtocolSupport.debugMode) {
                                        ProxyProtocolSupport.infoLogger.accept("[代理协议] IP被白名单匹配器允许: " + matcher.toString());
                                    }
                                    isWhitelistedIP = true;
                                    break;
                                } else if (ProxyProtocolSupport.debugMode) {
                                    ProxyProtocolSupport.infoLogger.accept("[代理协议] IP不匹配匹配器: " + matcher.toString());
                                }
                                i++;
                            }

                            if (!isWhitelistedIP) {
                                ProxyProtocolSupport.warnLogger.accept("[代理协议] 已阻止代理IP: " + proxySocketAddress + " (不在白名单中)");
                                if (ctx.channel().isOpen()) {
                                    ctx.disconnect();
                                }
                                return;
                            } else if (ProxyProtocolSupport.debugMode) {
                                ProxyProtocolSupport.infoLogger.accept("[代理协议] 代理IP通过白名单检查");
                            }
                        } else {
                            ProxyProtocolSupport.warnLogger.accept("**********************************************************************");
                            ProxyProtocolSupport.warnLogger.accept("* 检测到非InetSocketAddress的其他SocketAddress类型!                    *");
                            ProxyProtocolSupport.warnLogger.accept("* 请将日志报告给模组作者以提供兼容性支持!                                *");
                            ProxyProtocolSupport.warnLogger.accept("* 作者仓库: https://github.com/PanSzelescik/proxy-protocol-support/issues      *");
                            ProxyProtocolSupport.warnLogger.accept("* 修改版仓库: https://github.com/Hydroline/proxy-protocol-support/issues      *");
                            ProxyProtocolSupport.warnLogger.accept("**********************************************************************");
                            ProxyProtocolSupport.warnLogger.accept(proxyAddress.getClass().toString());
                            ProxyProtocolSupport.warnLogger.accept(proxyAddress.toString());
                        }
                    } else if (ProxyProtocolSupport.debugMode && !ProxyProtocolSupport.whitelistMode) {
                        System.out.println("[代理协议] 自动检测模式: 接受来自任何IP的HAProxy消息");
                        ProxyProtocolSupport.infoLogger.accept("[代理协议] 自动检测模式: HAProxy消息已接受，来自: " + proxyAddress);
                    }

                    if (ProxyProtocolSupport.debugMode) {
                        System.out.println("[代理协议] 在NetworkManager上设置真实地址...");
                        ProxyProtocolSupport.infoLogger.accept("[代理协议] 在NetworkManager上设置真实地址...");
                    }
                    ((ProxyProtocolAddressSetter) connection).setAddress(socketAddr);
                    
                    ProxyProtocolSupport.infoLogger.accept("[代理协议] 代理连接已建立 - 真实IP: " + realAddress + ":" + realPort + " | 代理: " + proxyAddress);
                } else if (ProxyProtocolSupport.debugMode) {
                    System.out.println("[代理协议] 收到LOCAL命令，跳过地址覆盖");
                    ProxyProtocolSupport.infoLogger.accept("[代理协议] 收到LOCAL命令，跳过地址覆盖");
                }
                          } catch (Exception e) {
                ProxyProtocolSupport.exceptionLogger.accept("[代理协议] 处理HAProxyMessage时发生错误!", e);
                e.printStackTrace();
            } finally {
                if (msg instanceof HAProxyMessage) {
                    try {
                        if (ProxyProtocolSupport.debugMode) {
                            ProxyProtocolSupport.infoLogger.accept("[代理协议] 尝试释放HAProxyMessage资源");
                        }
                        ((HAProxyMessage) msg).release();
                        if (ProxyProtocolSupport.debugMode) {
                            ProxyProtocolSupport.infoLogger.accept("[代理协议] HAProxyMessage资源释放成功");
                        }
                    } catch (NoSuchMethodError e) {
                        if (ProxyProtocolSupport.debugMode) {
                            System.out.println("[代理协议] HAProxyMessage.release()在此Netty版本中不可用 (MC 1.16.5预期行为)");
                            ProxyProtocolSupport.infoLogger.accept("[代理协议] HAProxyMessage.release()在此Netty版本中不可用 (MC 1.16.5预期行为)");
                        }
                    } catch (Exception e) {
                        if (ProxyProtocolSupport.debugMode) {
                            ProxyProtocolSupport.infoLogger.accept("[代理协议] 释放HAProxyMessage时发生错误: " + e.getMessage());
                        }
                    }
                    
                    if (ProxyProtocolSupport.debugMode) {
                        System.out.println("[代理协议] 从管道中移除ProxyProtocolHandler (HAProxy处理完成)");
                        ProxyProtocolSupport.infoLogger.accept("[代理协议] 从管道中移除ProxyProtocolHandler");
                    }
                    ctx.pipeline().remove(this);
                }
            }
        } else {
            if (ProxyProtocolSupport.allowDirectConnections) {
                if (ProxyProtocolSupport.debugMode) {
                    System.out.println("[代理协议] 混合模式: 检测到非HAProxy消息，允许直连");
                    ProxyProtocolSupport.infoLogger.accept("[代理协议] 混合模式: 允许直连，移除代理处理器");
                }
                
                try {
                    ctx.pipeline().remove("haproxy-decoder");
                    ctx.pipeline().remove("haproxy-handler");
                    if (ProxyProtocolSupport.debugMode) {
                        System.out.println("[代理协议] 为直连移除HAProxy处理器");
                        ProxyProtocolSupport.infoLogger.accept("[代理协议] 为直连移除HAProxy处理器");
                    }
                } catch (Exception e) {
                    if (ProxyProtocolSupport.debugMode) {
                        System.out.println("[代理协议] 移除HAProxy处理器时发生错误: " + e.getMessage());
                        ProxyProtocolSupport.infoLogger.accept("[代理协议] 警告: 移除HAProxy处理器时发生错误: " + e.getMessage());
                    }
                }
                
                super.channelRead(ctx, msg);
            } else {
                if (ProxyProtocolSupport.debugMode) {
                    System.out.println("[代理协议] 纯代理模式: 拒绝非HAProxy消息");
                }
                ProxyProtocolSupport.warnLogger.accept("[代理协议] 纯代理模式: 直连被拒绝");
                if (ctx.channel().isOpen()) {
                    ctx.disconnect();
                }
            }
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (ProxyProtocolSupport.debugMode) {
            System.out.println("[代理协议] === 通道非活跃 ===");
            System.out.println("[代理协议] 通道已断开: " + ctx.channel().remoteAddress());
            ProxyProtocolSupport.infoLogger.accept("[代理协议] 通道已断开: " + ctx.channel().remoteAddress());
        }
        super.channelInactive(ctx);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if (ProxyProtocolSupport.debugMode) {
            System.out.println("[代理协议] === 捕获异常 ===");
            System.out.println("[代理协议] 通道: " + ctx.channel().remoteAddress());
            System.out.println("[代理协议] 异常: " + cause.getMessage());
            cause.printStackTrace();
        }
        if (cause instanceof Exception) {
            ProxyProtocolSupport.exceptionLogger.accept("[代理协议] 处理器异常，通道 " + ctx.channel().remoteAddress(), (Exception) cause);
        } else {
            ProxyProtocolSupport.errorLogger.accept("[代理协议] 处理器Throwable，通道 " + ctx.channel().remoteAddress() + ": " + cause.getMessage());
        }
        super.exceptionCaught(ctx, cause);
    }
}
