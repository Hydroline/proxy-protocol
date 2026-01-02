package com.hydroline.proxy.protocol.shared.impl;

import io.netty.channel.Channel;

public interface IChannelInitializer {

    void invokeInitChannel(Channel ch) throws Exception;
}
