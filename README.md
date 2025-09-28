# Proxy Protocol Support
> This version is for Minecraft 1.20.1

Proxy Protocol Support is a [Fabric](https://fabricmc.net/) and [Quilt](https://quiltmc.org/) mod which adds support for [Proxy Protocol (HAProxy)](https://www.haproxy.com/blog/haproxy/proxy-protocol/ "Proxy Protocol (HAProxy)") for your Minecraft server.

For example you can use [TCPShield](https://tcpshield.com/ "TCPShield") or other software ([Nginx](https://nginx.org/en/docs/stream/ngx_stream_proxy_module.html#proxy_protocol "Nginx")) to forward traffic, and hide your server's IP address. Without Proxy Protocol you can see in console Proxy's IP address. Using and reading Proxy Protocol packet makes showing player's IP address possible.

In our setup, we use a Tencent Lighthouse (LH) server as a reverse proxy in front of a Minecraft server running on our home network to hide the origin server’s real IP address. Players connect to the LH server, while the actual Minecraft server’s IP remains concealed behind it. With this mod, we can still obtain players’ real IP addresses (such as bukkit plugins that need to get player's IP address) even when the server is behind the reverse proxy.