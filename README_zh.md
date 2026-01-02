# Proxy Protocol Support

> [英文](README.md)

本项目基于 [PanSzelescik/proxy-protocol-support](https://github.com/PanSzelescik/proxy-protocol-support)。

**Proxy Protocol Support** 是一个 **Forge 模组**，用于在 Minecraft 服务器部署在 **HAProxy / TCPShield / Nginx** 等 TCP 反向代理之后，仍然保留并获取客户端的**真实 IP 地址**。该模组会解析 **Proxy Protocol** 数据包，并将其中的内容暴露给插件使用，使下游代码能够读取客户端原始的 `SocketAddress`，而不是代理服务器的地址。

例如，你可以使用 **[TCPShield](https://tcpshield.com/)** 或其他软件（如 **[Nginx](https://nginx.org/en/docs/stream/ngx_stream_proxy_module.html#proxy_protocol)**）来转发网络流量，从而隐藏服务器的真实 IP 地址。
在未启用 Proxy Protocol 的情况下，服务器控制台和插件中只能看到**代理服务器的 IP**；而通过解析并读取 Proxy Protocol 数据包，则可以正确获取并显示玩家的真实 IP 地址。

在我们的实际部署中，我们使用 **腾讯云 Lighthouse（LH）服务器** 作为反向代理，放置在运行于家庭网络中的 Minecraft 服务器前端，用于隐藏源服务器的真实 IP 地址。玩家连接到的是 LH 服务器，而真正的 Minecraft 服务器 IP 则被完全隐藏在其后。借助本模组，即使服务器位于反向代理之后，我们仍然可以获取玩家的真实 IP 地址（例如供需要玩家 IP 信息的 Bukkit 插件使用）。

## 架构布局

- 根目录：`build.gradle`/`settings.gradle`/`gradlew`/`gradle.properties` 负责协调三个版本、统一 Proxy 配置，并通过 `proxy-properties.gradle` 填充上下游模块。
- `forge-1.16.5/`：Forge 1.16.5 子工程（ForgeGradle 4.x、Java 8），含旧版本的 mixin/handler。
- `forge-1.18.2/`：Forge 1.18.2 子工程（ForgeGradle 5.x、Java 17），提供 1.18.2 映射与兼容代码。
- `forge-1.20.1/`：Forge 1.20.1 子工程（ForgeGradle 6.x、Java 17），构建最新的 jar/shadow 产物。

## 构建方式

- 根目录的 `./gradlew buildAllTargets` 可用于一次性生成所有版本；它会根据版本选择 Java 8/17，也可以通过设置 `JAVA_HOME_8`/`JAVA_HOME_17` 来覆盖。
- 如果只想单独构建某个版本：进入 `forge-1.xx` 目录执行 `./gradlew build`，每个子工程会自动加载父级 `proxy-properties.gradle`，复用统一的代理设置。
- 代理配置集中在根目录的 `gradle.properties`；CI 和子模块启动前都会设置这些 `systemProp.*`，无需在每个子工程重复写。
- GitHub Actions 仅按版本逐个运行构建，每个 job 上传 `proxy-protocol-<version>` 产物。
