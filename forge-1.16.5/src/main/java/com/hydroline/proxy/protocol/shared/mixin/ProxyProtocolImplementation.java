package com.hydroline.proxy.protocol.shared.mixin;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import com.hydroline.proxy.protocol.shared.impl.IChannelInitializer;
import com.hydroline.proxy.protocol.shared.impl.ProxyProtocolChannelInitializer;
import com.hydroline.proxy.protocol.shared.impl.ProxyProtocolFallbackInitializerInvoker;

/**
 * Replaces anonymous ChannelInitializer with ProxyProtocolChannelInitializer
 *
 * @author PanSzelescik
 * @see ProxyProtocolChannelInitializer
 */
@Pseudo
@Mixin(targets = "net.minecraft.server.network.ServerConnectionListener", remap = false)
public class ProxyProtocolImplementation {

    @Redirect(method = "startTcpServerListener", at = @At(value = "INVOKE", target = "Lio/netty/bootstrap/ServerBootstrap;childHandler(Lio/netty/channel/ChannelHandler;)Lio/netty/bootstrap/ServerBootstrap;", remap = false))
    private ServerBootstrap addProxyProtocolSupport(ServerBootstrap bootstrap, ChannelHandler childHandler) {
        IChannelInitializer channelInitializer;
        if (childHandler instanceof IChannelInitializer) {
            channelInitializer = (IChannelInitializer) childHandler;
        } else {
            channelInitializer = new ProxyProtocolFallbackInitializerInvoker(childHandler);
        }
        return bootstrap.childHandler(new ProxyProtocolChannelInitializer(channelInitializer));
    }
}
