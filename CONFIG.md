# Proxy Protocol Support - 配置指南

## 目录
- [配置文件位置](#配置文件位置)
- [配置文件格式](#配置文件格式)
- [配置选项详解](#配置选项详解)
- [工作模式](#工作模式)
- [常见配置场景](#常见配置场景)
- [故障排除](#故障排除)

## 配置文件位置

配置文件会在服务器首次启动时自动创建：

```
服务器根目录/
├── config/
│   └── proxy_protocol_support.json  ← 配置文件
├── mods/
│   └── proxy_protocol_support-1.2.0-forge-1.16.5.jar
└── ...
```

## 配置文件格式

配置文件使用JSON格式，默认配置：

```json
{
  "enable-proxy-protocol": false,
  "allow-direct-connections": true,
  "whitelist-mode": false,
  "debug-mode": false,
  "proxy-protocol-whitelisted-ips": [
    "127.0.0.1/32",
    "::1/128"
  ],
  "whitelistTCPShieldServers": false
}
```

## 配置选项详解

### `enable-proxy-protocol`
- **类型**: `boolean`
- **默认值**: `false`
- **说明**: 是否启用Proxy Protocol支持
- **影响**: 
  - `true`: 启用代理协议处理
  - `false`: 禁用代理协议处理

### `allow-direct-connections`
- **类型**: `boolean`
- **默认值**: `true`
- **说明**: 是否允许直连用户（非代理连接）
- **影响**:
  - `true`: 允许直连用户正常连接
  - `false`: 拒绝所有非代理连接

### `whitelist-mode`
- **类型**: `boolean`
- **默认值**: `false`
- **说明**: 是否启用IP白名单模式
- **推荐**: 使用默认值`false`（自动检测模式）
- **影响**:
  - `false`: 自动检测模式 - 自动识别Proxy Protocol头，无需配置白名单（推荐）
  - `true`: 白名单模式 - 只允许白名单中的IP发送Proxy Protocol头

### `debug-mode`
- **类型**: `boolean`
- **默认值**: `false`
- **说明**: 是否启用调试模式
- **影响**:
  - `true`: 输出详细的调试日志
  - `false`: 只输出关键信息

### `proxy-protocol-whitelisted-ips`
- **类型**: `string[]`
- **默认值**: `["127.0.0.1/32", "::1/128"]`
- **说明**: 允许发送Proxy Protocol头的IP地址或域名白名单
- **格式**: 
  - 支持IP地址：`192.168.1.1`
  - 支持CIDR格式：`192.168.1.0/24`
  - 支持域名：`proxy.example.com`（启动时自动解析为IP）
  - 支持IPv6：`::1/128`
- **域名支持**:
  - 域名会在服务器启动时自动解析为IP地址
  - 如果域名对应的IP发生变化，需要重启服务器
  - DNS解析失败会导致服务器启动失败
- **示例**:
  ```json
  [
    "127.0.0.1/32",           // 本地IPv4
    "::1/128",                // 本地IPv6
    "10.0.0.1/32",            // 单个IP
    "192.168.1.0/24",         // 网段
    "43.138.4.73/32",         // 代理服务器IP
    "proxy.example.com",      // 域名（自动解析）
    "cdn.cloudflare.com/32"   // 域名+CIDR（域名解析后应用掩码）
  ]
  ```

### `whitelistTCPShieldServers`
- **类型**: `boolean`
- **默认值**: `false`
- **说明**: 是否自动添加TCPShield服务器IP到白名单
- **注意**: 仅在使用TCPShield服务时启用

## 工作模式

### 混合+自动检测模式（推荐）
**自动识别代理和直连，无需配置白名单**

```json
{
  "enable-proxy-protocol": true,
  "allow-direct-connections": true,
  "whitelist-mode": false,
  "debug-mode": false,
  "proxy-protocol-whitelisted-ips": []
}
```

**特点**:
- 自动识别：自动检测是否有Proxy Protocol头
- 无需白名单：不需要手动配置IP地址
- 完美兼容：代理用户获取真实IP，直连用户正常连接  
- 零配置：适合99%的使用场景
- 动态IP友好：代理服务器IP变化时无需修改配置

### 混合+白名单模式
**同时支持代理和直连，但限制代理IP**

```json
{
  "enable-proxy-protocol": true,
  "allow-direct-connections": true,
  "whitelist-mode": true,
  "debug-mode": false,
  "proxy-protocol-whitelisted-ips": [
    "43.138.4.73/32",
    "127.0.0.1/32",
    "::1/128"
  ]
}
```

### 仅代理+自动检测模式
**只允许代理连接，但自动识别**

```json
{
  "enable-proxy-protocol": true,
  "allow-direct-connections": false,
  "whitelist-mode": false,
  "debug-mode": false
}
```

### 仅代理+白名单模式
**只允许特定IP的代理连接**

```json
{
  "enable-proxy-protocol": true,
  "allow-direct-connections": false,
  "whitelist-mode": true,
  "debug-mode": false,
  "proxy-protocol-whitelisted-ips": [
    "43.138.4.73/32"
  ]
}
```

### 禁用模式
**完全禁用代理协议**

```json
{
  "enable-proxy-protocol": false,
  "allow-direct-connections": true,
  "debug-mode": false
}
```

## 常见配置场景

### nginx反向代理

**nginx配置**:
```nginx
stream {
    upstream minecraft {
        server your-minecraft-server:25565;
    }
    
    server {
        listen 25565;
        proxy_pass minecraft;
        proxy_protocol on;
        proxy_timeout 600s;
        proxy_responses 1;
    }
}
```

**Minecraft配置**:
```json
{
  "enable-proxy-protocol": true,
  "allow-direct-connections": true,
  "whitelist-mode": false,
  "debug-mode": false,
  "proxy-protocol-whitelisted-ips": [
    "43.138.4.73/32",
    "127.0.0.1/32",
    "::1/128"
  ]
}
```

### 云服务器负载均衡

```json
{
  "enable-proxy-protocol": true,
  "allow-direct-connections": true,
  "whitelist-mode": true,
  "debug-mode": false,
  "proxy-protocol-whitelisted-ips": [
    "10.0.0.0/8",
    "172.16.0.0/12",
    "192.168.0.0/16",
    "127.0.0.1/32",
    "::1/128"
  ]
}
```

### TCPShield防护

```json
{
  "enable-proxy-protocol": true,
  "allow-direct-connections": false,
  "whitelist-mode": true,
  "debug-mode": false,
  "whitelistTCPShieldServers": true,
  "proxy-protocol-whitelisted-ips": [
    "127.0.0.1/32",
    "::1/128"
  ]
}
```

### 开发测试环境

```json
{
  "enable-proxy-protocol": false,
  "allow-direct-connections": true,
  "debug-mode": true
}
```

## 故障排除

### 直连用户无法连接
**解决方案**: 确保 `allow-direct-connections: true`

### 代理用户无法连接
**检查项目**:
1. 确认代理服务器IP在白名单中
2. 确认nginx/HAProxy正确配置了`proxy_protocol on`
3. 启用debug模式查看详细日志

### 无法获取真实IP
**检查项目**:
1. 确认`enable-proxy-protocol: true`
2. 确认nginx/HAProxy配置正确
3. 检查白名单是否包含代理服务器IP

### 域名解析失败
**解决方案**:
```bash
# 手动测试域名解析
nslookup proxy.example.com
ping proxy.example.com
```

**临时解决方案**: 使用IP代替域名

### 调试建议
1. 启用debug模式：`"debug-mode": true`
2. 查看服务器日志中的`[代理协议]`相关信息
3. 重启服务器使配置生效

## 配置验证

启动服务器后，查看日志确认配置正确加载：

```
[代理协议] 配置调试 - 启用代理协议: true
[代理协议] 配置调试 - 允许直连: true
[代理协议] 配置调试 - 白名单模式: false
[代理协议] 配置调试 - 调试模式: false
```

**注意**: 配置更改后需要重启Minecraft服务器才能生效。 