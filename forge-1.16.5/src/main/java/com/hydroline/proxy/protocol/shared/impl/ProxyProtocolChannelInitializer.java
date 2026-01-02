package com.hydroline.proxy.protocol.shared.impl;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.haproxy.HAProxyMessageDecoder;
import com.hydroline.proxy.protocol.shared.ProxyProtocolSupport;

/**
 * Initializes HAProxyMessageDecoder and ProxyProtocolHandler
 */
public class ProxyProtocolChannelInitializer extends ChannelInitializer<Channel> {

    private final IChannelInitializer channelInitializer;

    public ProxyProtocolChannelInitializer(IChannelInitializer channelInitializer) {
        this.channelInitializer = channelInitializer;
    }

    @Override
    protected void initChannel(Channel channel) throws Exception {
        ProxyProtocolSupport.logDebug("Initializing channel " + channel.remoteAddress());

        this.channelInitializer.invokeInitChannel(channel);

        if (!ProxyProtocolSupport.enableProxyProtocol) {
            ProxyProtocolSupport.logDebug("Proxy Protocol disabled, leaving channel " + channel.remoteAddress() + " untouched.");
            return;
        }

        ProxyProtocolSupport.logDebug("Proxy Protocol enabled, installing smart detector.");
        installSmartDetector(channel);
        ProxyProtocolSupport.logDebug("Smart detector installed for " + channel.remoteAddress());
    }

    private static void installSmartDetector(Channel channel) {
        final ChannelPipeline pipeline = channel.pipeline();
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
            ProxyProtocolSupport.logDebug("Pipeline before detector: " + pipeline.names());
            ProxyProtocolSupport.logDebug("Selected anchor: " + (anchor == null ? "<none>" : anchor));
        }

        try {
            SmartProxyProtocolDetector detector = new SmartProxyProtocolDetector();
            if (anchor == null) {
                pipeline.addFirst("smart-detector", detector);
            } else if ("packet_handler".equals(anchor)) {
                pipeline.addBefore(anchor, "smart-detector", detector);
            } else {
                pipeline.addAfter(anchor, "smart-detector", detector);
            }
        } catch (Exception e) {
            ProxyProtocolSupport.exceptionLogger.accept("Failed to install smart detector, falling back to addFirst.", e instanceof Exception ? (Exception) e : new RuntimeException(e));
            try {
                if (pipeline.get("smart-detector") == null) {
                    pipeline.addFirst("smart-detector", new SmartProxyProtocolDetector());
                }
            } catch (Exception fallbackEx) {
                ProxyProtocolSupport.warnLogger.accept("Smart detector fallback installation failed, skipping detector to keep connection alive.");
                ProxyProtocolSupport.logDebug("Fallback exception: " + fallbackEx.getMessage());
            }
        }
    }
}
