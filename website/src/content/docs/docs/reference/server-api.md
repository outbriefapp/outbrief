---
title: "Server 接口"
description: "outbrief-server 的 HTTP 接口、鉴权和数据保留规则。"
---

:::note
这一页给开发者看。请求和响应的结构以 [`src/protocol.ts`](https://github.com/outbriefapp/outbrief-server/blob/main/src/protocol.ts) 为准，`outbrief-app` 和 `outbrief-daemon` 里各有一份镜像类型，改接口时要一起改。
:::

除了公开接口，其他请求都要带设备令牌 `Authorization: Bearer <令牌>`（App 是 `oba_…`，daemon 是 `obm_…`）。

## 公开接口

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/healthz` | 健康检查（无需鉴权） |
| `GET` | `/v1/server` | 无需鉴权。`{ signup, push }`。`signup`：`open`（谁都能建账号）/ `claim`（还没人认领，第一个账号要带认领码）/ `closed`（只能用配对码加入）。`push`：这台 server 能用哪些推送叫醒手机（`["fcm"]`，没配就是 `[]`） |
| `POST` | `/v1/accounts` | 无需鉴权。`{ device: { name, kind }, claimCode? }` → `201 { accountId, device, token }`：建匿名账号和它的第一台设备；`403 signup_closed` / `invalid_claim_code`，`429 rate_limited` |
| `POST` | `/v1/pairing/redeem` | 无需鉴权。`{ code, device: { name, kind } }` → `201 { accountId, device, token }`：用 6 位配对码加入账号；`404 invalid_pairing_code`，`429 rate_limited` |

## 账号、设备与配对

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/v1/me` | 令牌所属的账号和设备（也用来检查令牌是否还有效） |
| `POST` | `/v1/pairing` | 生成配对码 `{ code, expiresAt }`（10 分钟，只能用一次） |
| `GET` | `/v1/pairing/:code` | 配对码有没有被用、被哪台设备用了（显示配对码的设备用它等待） |
| `GET` | `/v1/devices` | 本账号的所有设备（`kind`、`online`、`current`） |
| `DELETE` | `/v1/devices/:id` | 移除本账号的一台设备：令牌立即失效，连接断开，不再推送。别的账号的设备一律 `404` |
| `PUT` | `/v1/devices/me/push` | `{ provider: "fcm", token }` → `204`：来电时用这个 FCM 令牌叫醒这台手机（ADR 0010）。server 没配推送 `409 push_unavailable` |
| `DELETE` | `/v1/devices/me/push` | `204`：不再推送这台设备（关掉了「后台来电」） |
| `POST` | `/v1/devices/:id/settings` | `{ requestId, sealed }` → `{ sealed }`：把加密的设置请求转给这台电脑的 daemon，返回它加密的结果（手机改 daemon 设置、派单用）；`sealed` 最长 1.8 亿字符（派单的图片一张一张转，每张最大 100 MB，和 Multica 的上传上限一致，YOUT-226），其他密文仍是 200 万；`409 machine_offline`，`504 machine_timeout` |

## 来电

| 方法 | 路径 | 说明 |
|---|---|---|
| `POST` | `/v1/events` | 上报一条加密的汇报 `{ source, occurredAt?, sealed }`；返回 `201` + `AgentEvent`，立即推送给本账号的设备。明文字段（`content` 等）一律 `400` |
| `GET` | `/v1/events?after=<seq>` | 轮询：本账号 `seq` 之后仍待接听的汇报（密文） |
| `GET` | `/v1/events/:id` | 本账号的一通来电，和 SSE 推的一样：手机收到推送后用它取来电 |
| `POST` | `/v1/events/:id/receipt` | `{ via: "push" \| "stream" }` → `204`：这台设备已经知道这通来电（响了或进了未接），server 不再推送它 |
| `GET` | `/v1/stream` | SSE；事件名 `agent-event`，`id` = `seq`，支持 `Last-Event-ID` 续传；只推本账号的来电和投递状态；另推 `call-status`：某台设备接起或结束了一通来电（不带密文），还在响的设备停下；重放时不再推别的设备已接的来电 |
| `POST` | `/v1/events/:id/status` | 客户端回写 `completed` / `dismissed` / `acknowledged`（未接来电「全部知悉」），同时删掉这通来电的密文（`sealed: null`）。只有还在响、或由这台设备接起的来电能结束；别的设备已经接了 `409 answered_elsewhere`，已经结束 `409 already_ended`（同一台设备重复回写同一结果是 200） |
| `POST` | `/v1/events/:id/answer` | 这台设备接起来电。账号下所有设备同时响铃，第一台接的拿到来电（`handledBy`），别的设备收到 `call-status` 停止响铃；晚到的 `409 answered_elsewhere` |
| `POST` | `/v1/events/:id/reply` | 挂断后回复 `{ sealed }`：排队交给上报这通来电的机器的 daemon。daemon 解密后接着原会话运行 Agent，或者（Multica 汇报）用本机保存的 Multica 令牌发评论 |

## daemon 专用

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/v1/daemon` | daemon 令牌。WebSocket：下发回复、转发设置请求，收回执行结果 |
| `POST` | `/v1/daemon/events` | daemon 令牌。daemon 转发本机 Agent 的汇报 `{ source, occurredAt?, sealed }` |
| `POST` | `/v1/daemon/multica-reports` | daemon 令牌。daemon 读到的一个完成的 Multica 任务 `{ taskId, occurredAt?, sealed }`；同一账号里同一任务第二次上报 `409 duplicate_task` |

## 账号和设备

- 没有登录，也没有共享口令。账号只是一个 id；每台设备（App、手机、daemon）有自己的 Bearer 令牌（App `oba_…`，daemon `obm_…`），server 只存 SHA-256，可以在任意一台设备上逐台移除。`/v1/daemon/*` 只认 daemon 的令牌。
- 来电、回复、电脑、投递状态都按账号隔离，SSE 和 WebSocket 只推给同一账号的设备。
- 谁先装谁建账号，其他设备用 6 位配对码（10 分钟、一次性）加入。二维码 / 配对链接 `outbrief://pair?server=…&code=…&key=obk1_…` 同时带上端到端密钥，由设备直接传给设备，server 看不到。输错配对码、认领码会被限流。
- 谁能建账号取决于部署：公共云端设 `OUTBRIEF_OPEN_SIGNUP=true`，开放注册、限流；私有化部署默认关闭：还没有账号时 server 每次启动在日志里打印一次性**认领码**，第一个账号要填它，之后不再允许建新账号。原因见 ADR 0008。

服务端不保存、也不使用任何 Multica 令牌：Multica 令牌只存在用户电脑上的 outbrief-daemon 里，由它监听任务、上报汇报、发回复。请求 / 响应结构以 [`src/protocol.ts`](https://github.com/outbriefapp/outbrief-server/blob/main/src/protocol.ts) 为准；`outbrief-app` 与 `outbrief-daemon` 各有一份镜像类型，改接口时一起改。

简报（`Brief`）由 outbrief-daemon 用本机配置的大模型生成，和汇报一起加密后提交。`protocol.ts` 里的 `SealedReport` / `SealedReply` / `Brief` 定义的是密文里面的内容，是 daemon 和客户端之间的约定，服务端既看不到也不校验。

## 端到端加密

- 汇报（标题、原文、`cwd`、`sessionId`、简报、Multica issue 信息）由 daemon 加密成 `sealed`；回复（内容和回到哪个会话 / 评论）由客户端加密；回复失败的原因由 daemon 加密。算法 AES-256-GCM，格式 `ob1.<keyId>.<iv>.<密文>`，见 ADR 0007。
- 密钥只在用户的 daemon 和客户端上：daemon 第一次启动随机生成，本机桌面端自动同步，也可以在客户端改成口令。服务端从来拿不到。
- 服务端看得到的明文只有路由要用的：来源、时间、上报的机器、Multica 任务 id（同一任务只来一次电）、状态、投递状态、Multica 回复评论 id。

## 来电推送

手机不在事件流上时（App 被划掉、手机休眠、换网络），来电由 server 推送叫醒它（[ADR 0010](/docs/adr/0010-wake-phones-with-push/)）：

- 来电写库的同一个事务里，给本账号每台登记了 FCM 令牌的设备排一条推送（`push_jobs`），提交后马上发；server 在两步之间重启也不会丢。
- 推送里没有内容，只有来电的 id、`seq` 和收到时间；手机拿到后用自己的令牌取密文，在本机解密、响铃。
- 手机知道这通来电后回一个回执（`/v1/events/:id/receipt`）。没有回执就每 10 秒再推一次，最多 3 次；来电被接起、结束，或者过了 2 分钟的响铃窗口就不再推。
- 推送用 Firebase Cloud Messaging，要 Android App 用同一个 Firebase 项目打包。打包和装好之后怎么确认，见 App 文档的[「Android 安装包」](/docs/development/#android-安装包)和[「手机在后台或锁屏时响铃」](/docs/guides/background-calls/)。

## 服务端留下什么

- 待接听的来电：密文，直到某个客户端回写 `completed` / `dismissed` / `acknowledged`（未接来电「全部知悉」），然后删掉。
- 回复：排队期间保存密文，投递成功或失败后删掉，只留状态和（加密的）错误原因。
- 推送：每台手机的 FCM 令牌；每次推送和回执的时间、结果，一天后删掉。
- 历史记录只在客户端本机。
