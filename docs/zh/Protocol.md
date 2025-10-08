# 协议

## 客户端消息

任何客户端消息都有一个 4 字节的头部：

| off | len | type     | identifier   | name     |
| --- | --- | -------- | ------------ | -------- |
| `0` | `1` | `'H'`    | `type`       | 消息类型 |
| `1` | `3` | `u24_be` | `request_id` | 请求标识 |

头部之后的所有数据都属于身体（body），可空

### 心跳 Heartbeat

#### 请求格式

| off | len | type     | identifier   | name     |
| --- | --- | -------- | ------------ | -------- |
| `0` | `1` | `'H'`    | `type`       | 消息类型 |
| `1` | `3` | `u24_be` | `request_id` | 请求标识 |

#### 预期响应

- kind: `OK`
- body: none

### 查询 Query

#### 请求格式

| off | len   | type         | identifier   | name     |
| --- | ----- | ------------ | ------------ | -------- |
| `0` | `1`   | `'Q'`        | `type`       | 消息类型 |
| `1` | `3`   | `u24_be`     | `request_id` | 请求标识 |
| `4` | `4+?` | `sized_utf8` | `plugin_id`  | 插件标识 |
| `?` | `4+?` | `sized_utf8` | `method_id`  | 方法标识 |
| `?` | `?`   | `bytes`      | `payload`    | 负载     |

#### 预期响应

- kind: `OK`
- body: `bytes`

### 订阅事件 Subscription

#### 请求格式

| off | len   | type         | identifier   | name                                                                                               |
| --- | ----- | ------------ | ------------ | -------------------------------------------------------------------------------------------------- |
| `0` | `1`   | `'S'`        | `type`       | 消息类型                                                                                           |
| `1` | `3`   | `u24_be`     | `request_id` | 请求标识                                                                                           |
| `4` | `4+?` | `sized_utf8` | `plugin_id`  | 插件标识。前 4 字节是`i32`，表示 utf8 字符串的字节数，接下来这么多字节是 utf8 字符串，表示插件标识 |
| `?` | `4+?` | `sized_utf8` | `event_id`   | 事件标识                                                                                           |
| `?` | `?`   | `bytes`      | `condition`  | 条件                                                                                               |

#### 预期响应

- kind: `OK`
- body: `u32_be`，订阅标识

如果多次重复订阅同一事件，则返回同一订阅标识，效果与订阅一次相同。

### 取消订阅 Unsubscription

#### 请求格式

| off | len | type     | id             | name     |
| --- | --- | -------- | -------------- | -------- |
| `0` | `1` | `'U'`    | `type`         | 消息类型 |
| `1` | `3` | `u24_be` | `request_id`   | 请求标识 |
| `1` | `4` | `u32_be` | `subscribe_id` | 订阅标识 |

#### 预期响应

- 类型：`OK`

### 列出插件

| off | len | type     | identifier   | name     |
| --- | --- | -------- | ------------ | -------- |
| `0` | `1` | `'P'`    | `type`       | 消息类型 |
| `1` | `3` | `u24_be` | `request_id` | 请求标识 |

#### 预期响应

- 类型：`OK`
- 负载：`JsonArray`

负载类型：

```ts
type ListPluginResponse = string[];
```

### 批量请求 Batch

#### 请求格式

| off  | len | type        | identifier   | name                 |
| ---- | --- | ----------- | ------------ | -------------------- |
| `0`  | `1` | `'B'`       | `type`       | 消息类型             |
| `1`  | `3` | `u24_be`    | `request_id` | 请求标识             |
| `4`  | `8` | `u64_be`    | `batch_size` | 批大小，请求列表长度 |
| `12` | `?` | `Request[]` | `requests`   | 请求列表中的元素们   |

请求列表由以下格式的元素组成：

| off | len       | type     | identifier | name                                                |
| --- | --------- | -------- | ---------- | --------------------------------------------------- |
| `0` | `8`       | `u64_be` | `length`   | 请求消息长度                                        |
| `9` | `$length` | `bin`    | `message`  | 请求消息，其格式可以是任意客户端消息，但没有请求 ID |

#### 预期响应

- 类型：`OK`
- 负载：

| off | len | type        | identifier | name               |
| --- | --- | ----------- | ---------- | ------------------ |
| `0` | `?` | `Request[]` | `requests` | 请求列表中的元素们 |

每个元素格式如下：

| off | len       | type     | identifier | name                              |
| --- | --------- | -------- | ---------- | --------------------------------- |
| `0` | `8`       | `u64_be` | `length`   | 响应消息长度                      |
| `9` | `$length` | `bin`    | `message`  | 响应消息内容，可以是`Ok`或`Error` |

## 服务端消息

任何服务端消息的长度都不小于 4 字节。

| off | len | type     | identifier | name     |
| --- | --- | -------- | ---------- | -------- |
| `0` | `1` | `'H'`    | `type`     | 消息类型 |
| `1` | `3` | `u24_be` | `any`      |          |

头部之后的所有数据都属于身体（body），可空

### 成功 Ok

| off | len | type     | identifier   | name     |
| --- | --- | -------- | ------------ | -------- |
| `0` | `1` | `'O'`    | `type`       | 消息类型 |
| `1` | `3` | `u24_be` | `request_id` | 请求标识 |
| `4` | `?` | `?`      | `payload`    | 负载     |

客户端会根据请求标识找到相应的请求记录，使用相应的方法来处理负载。所以负载的格式取决于相关请求的具体定义。

- 负载长度可以是 0
- 负载的格式可以是二进制数据，也可以是 JSON 数据

### 错误 Error

| off | len | type     | identifier   | name     |
| --- | --- | -------- | ------------ | -------- |
| `0` | `1` | `'E'`    | `type`       | 消息类型 |
| `1` | `3` | `u24_be` | `request_id` | 请求标识 |
| `4` | `1` | `u8_be`  | `error_code` | 错误码   |
| `5` | `?` | `bytes`  | `reason`     |          |

错误类型：

> 此表格需要更新，请查看 Java 服务端实现的源码
> `io.github.leawind.resonator.core.message.ServerMessage.ErrorKind`

| name                  | code   |
| --------------------- | ------ |
| `Other`               | `0x00` |
| `UnknownQuery`        | `0x10` |
| `InvalidQuery`        | `0x11` |
| `ErrorQuery`          | `0x12` |
| `UnknownSubscription` | `0x20` |

### 事件推送 Emit

| off | len | type     | id             | name     |
| --- | --- | -------- | -------------- | -------- |
| `0` | `1` | `'e'`    | `type`         | 消息类型 |
| `1` | `3` | `u24_be` | `subscribe_id` | 订阅标识 |
| `4` | `?` | `bytes`  | `event_data`   | 事件数据 |
