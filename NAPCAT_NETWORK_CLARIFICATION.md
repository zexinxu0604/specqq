# NapCat 网络配置说明

## NapCat 服务器位置

**重要**: NapCat 服务器运行在 **独立的服务器** 上，而不是 Docker 宿主机。

### 实际配置
- **NapCat 服务器 IP**: `192.168.215.2`
- **HTTP 端口**: `3000`
- **WebSocket 端口**: `3001`
- **HTTP Token**: `pDcIldXJcsTlEYxy`
- **WebSocket Token**: `RqgRI~2H2v_2WHbR`

### 网络拓扑

```
┌─────────────────────┐
│   开发机器/Docker    │
│   (运行此项目)       │
│                     │
│   Backend Service   │
│   Port: 8080        │
└──────────┬──────────┘
           │
           │ 通过网络连接
           │
           ▼
┌─────────────────────┐
│  NapCat 服务器       │
│  IP: 192.168.215.2  │
│                     │
│  HTTP:  :3000       │
│  WebSocket: :3001   │
└─────────────────────┘
```

## 配置文件更新

所有配置文件已更新为指向实际的 NapCat 服务器地址：

### 1. `.env` (开发环境实际配置)
```bash
NAPCAT_HTTP_URL=http://192.168.215.2:3000
NAPCAT_WS_URL=ws://192.168.215.2:3001
NAPCAT_HTTP_TOKEN=pDcIldXJcsTlEYxy
NAPCAT_WS_TOKEN=RqgRI~2H2v_2WHbR
```

### 2. `.env.example` (模板)
```bash
# HTTP API地址 (修改为你的NapCat服务器地址)
NAPCAT_HTTP_URL=http://192.168.215.2:3000
# WebSocket地址 (修改为你的NapCat服务器地址)
NAPCAT_WS_URL=ws://192.168.215.2:3001
```

### 3. `application-dev.yml` (开发环境默认值)
```yaml
napcat:
  http:
    url: ${NAPCAT_HTTP_URL:http://192.168.215.2:3000}
  websocket:
    url: ${NAPCAT_WS_URL:ws://192.168.215.2:3001}
```

### 4. `application-prod.yml` (生产环境默认值)
```yaml
napcat:
  http:
    url: ${NAPCAT_HTTP_URL:http://192.168.215.2:3000}
  websocket:
    url: ${NAPCAT_WS_URL:ws://192.168.215.2:3001}
```

### 5. `docker-compose.yml` (Docker 环境默认值)
```yaml
environment:
  NAPCAT_HTTP_URL: ${NAPCAT_HTTP_URL:-http://192.168.215.2:3000}
  NAPCAT_WS_URL: ${NAPCAT_WS_URL:-ws://192.168.215.2:3001}
```

## 为什么不使用 `host.docker.internal`？

- **`host.docker.internal`**: 这是 Docker 提供的特殊 DNS 名称，指向 **Docker 宿主机**
- **你的情况**: NapCat 运行在 `192.168.215.2`，这是一个 **独立的服务器**，不是 Docker 宿主机

### 网络连接方式

#### 场景 1: 本地开发（不使用 Docker）
```bash
开发机器 → 192.168.215.2:3000/3001
```
- 直接通过 IP 地址连接到 NapCat 服务器
- 使用 `.env` 文件中的配置

#### 场景 2: Docker 容器运行
```bash
Docker 容器 → 192.168.215.2:3000/3001
```
- Docker 容器通过网络连接到外部服务器
- 确保 Docker 网络可以访问 `192.168.215.2`
- 可能需要配置 Docker 网络模式（bridge/host）

## 网络连接验证

### 从开发机器测试连接
```bash
# 测试 HTTP 端口
curl -v http://192.168.215.2:3000

# 测试 WebSocket 端口（使用 wscat 或 websocat）
wscat -c ws://192.168.215.2:3001
```

### 从 Docker 容器测试连接
```bash
# 进入容器
docker exec -it chatbot-backend bash

# 测试连接
curl -v http://192.168.215.2:3000
```

## 潜在问题和解决方案

### 问题 1: 无法连接到 192.168.215.2
**可能原因**:
- 防火墙阻止连接
- NapCat 服务器未启动
- 网络路由问题

**解决方案**:
```bash
# 检查网络连通性
ping 192.168.215.2

# 检查端口是否开放
telnet 192.168.215.2 3000
telnet 192.168.215.2 3001

# 检查防火墙规则（在 NapCat 服务器上）
sudo ufw status
sudo firewall-cmd --list-all
```

### 问题 2: Docker 容器无法访问外部 IP
**可能原因**:
- Docker 网络配置限制

**解决方案**:
```yaml
# 在 docker-compose.yml 中使用 host 网络模式（仅 Linux）
services:
  backend:
    network_mode: "host"
```

或者确保 bridge 网络可以访问外部网络：
```yaml
networks:
  chatbot-network:
    driver: bridge
    driver_opts:
      com.docker.network.bridge.enable_ip_masquerade: "true"
```

### 问题 3: 认证失败
**可能原因**:
- Token 不正确
- NapCat 配置未启用 token 认证

**解决方案**:
1. 检查 NapCat 配置文件中的 token 设置
2. 确认 HTTP 和 WebSocket 使用正确的 token
3. 查看应用日志中的认证错误信息

## NapCat 服务器配置参考

在 NapCat 服务器上，配置文件应该类似：

```json
{
  "http": {
    "enable": true,
    "host": "0.0.0.0",
    "port": 3000,
    "secret": "pDcIldXJcsTlEYxy"
  },
  "ws": {
    "enable": true,
    "host": "0.0.0.0",
    "port": 3001,
    "secret": "RqgRI~2H2v_2WHbR"
  }
}
```

## 总结

- ✅ NapCat 服务器地址: `192.168.215.2`
- ✅ 不使用 `host.docker.internal`（那是指向 Docker 宿主机的）
- ✅ 所有配置文件已更新为使用实际服务器 IP
- ✅ 支持本地开发和 Docker 部署两种场景
- ⚠️ 确保网络连通性和防火墙配置正确

---

**更新日期**: 2026-02-10
**配置状态**: ✅ 已更新为独立 NapCat 服务器地址
