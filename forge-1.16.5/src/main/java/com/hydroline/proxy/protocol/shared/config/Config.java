package com.hydroline.proxy.protocol.shared.config;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class which represents configuration file
 *
 * @author PanSzelescik
 * @see Configuration
 */
public class Config {

    @SerializedName("enable-proxy-protocol")
    public boolean enableProxyProtocol = false;

    @SerializedName("proxy-protocol-whitelisted-ips")
    public List<String> whitelistedIPs = new ArrayList<>(Arrays.asList(
        "127.0.0.1/32",
        "::1/128"
    ));

    @SerializedName("whitelistTCPShieldServers")
    public boolean whitelistTCPShieldServers = false;
    
    @SerializedName("allow-direct-connections")
    public boolean allowDirectConnections = true;
    
    @SerializedName("whitelist-mode")
    public boolean whitelistMode = false;
    
    @SerializedName("debug-mode")
    public boolean debugMode = false;
}
