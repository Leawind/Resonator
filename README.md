中文 | [English][en]

---

# Resonator

Resonator 是一个基于 WebSocket 的插件化通信框架，支持客户端与服务端之间的查询和事件订阅推送机制。

## 核心功能

### 查询机制

客户端可以向服务端发起查询请求，指定插件和方法，并传递参数：

```mermaid
sequenceDiagram
    participant A as 应用(ts, py, ...)
    participant CP as 客户端插件
    participant C as 客户端
    participant S as 服务端
    participant SP as 服务端插件

    A ->> CP: 调用插件方法
    CP->> C: 调用查询方法
    C ->> S: 发送查询请求(插件ID, 方法ID, 参数)
    activate S
    S ->> SP: 调用对应插件方法
    SP-->>S: 返回执行结果
    S -->>C: 返回查询结果
    deactivate S
    C -->>A: 返回查询结果
```

### 事件订阅与推送

客户端可以订阅服务端事件，当事件发生时服务端会主动推送：

```mermaid
sequenceDiagram
    participant A as 应用(ts, py, ...)
    participant CP as 客户端插件
    participant C as 客户端
    participant S as 服务端
    participant SP as 服务端插件

    A ->> CP: 调用插件方法订阅事件
    CP->>C: 调用

    C->>S: 发送订阅请求(插件ID, 事件ID)
    activate S
    S->>SP: 注册事件监听器
    S-->> C: 返回订阅ID
    deactivate S

    loop 当事件发生时
      SP-->>S: 事件触发
      S-->>C: 推送事件数据
      C-->>A: 触发回调
    end
```

## 许可证

本项目采用 [MIT](./LICENSE) 许可证。

[zh]: README.zh.md
[en]: README.md
