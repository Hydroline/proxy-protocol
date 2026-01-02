package com.hydroline.proxy.protocol.shared.mixin;

import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.net.SocketAddress;

/**
 * Adds Accessor for address
 *
 * @author PanSzelescik
 */
@Mixin(NetworkManager.class)
public interface ProxyProtocolAddressSetter {

    @Accessor("socketAddress")
    void setAddress(SocketAddress address);
}
