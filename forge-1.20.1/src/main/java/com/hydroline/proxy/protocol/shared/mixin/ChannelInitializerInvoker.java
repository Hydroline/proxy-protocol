package com.hydroline.proxy.protocol.shared.mixin;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import com.hydroline.proxy.protocol.shared.impl.IChannelInitializer;

/**
 * Adds Invoker for initChannel
 *
 * @author PanSzelescik
 * @see io.netty.channel.ChannelInitializer#initChannel(Channel)
 */
@Mixin(ChannelInitializer.class)
public interface ChannelInitializerInvoker extends IChannelInitializer {

    @Invoker(value = "initChannel", remap = false)
    void invokeInitChannel(Channel ch) throws Exception;
}
