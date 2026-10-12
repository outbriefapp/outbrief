---
title: "Daemon 本机接口"
description: "outbrief-daemon 的本机 HTTP 接口、简报生成和发件箱机制。"
---

:::note
这一页给开发者看。日常使用时，这些设置都在 App 里改。
:::

## 鉴权

桌面端「设置 → Multica」「设置 → 大模型」直接请求本机 `http://127.0.0.1:8790`，令牌不经过 server。每个请求都要带本机密钥 `Authorization: Bearer <~/.outbrief/local-api.key 的内容>`：daemon 第一次 `run` 时生成这个文件（权限 600），只有这台电脑的当前用户读得到，桌面 App 通过 Tauri 读它，网页读不到；`Host` 必须是本机回环地址。

没有桌面 App 时（只有手机），同样的请求由手机用端到端密钥加密，经 server 的 `POST /v1/devices/<这台电脑>/settings` 通过 WebSocket 转给 daemon；daemon 解密、执行，把结果加密后原路返回，server 读不到内容。`/e2e/*` 和 `/local/*` 只能在本机调用，经 server 转发的一律 `403 not_relayed`；手机的密钥和这台电脑不一致时返回 `400 undecryptable_request`。

## Multica 设置

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/multica/settings` | `{ settings, status }`；`settings` 只带监听的工作区 `workspaces` 和 `tokenHint`，从不返回令牌；`status.workspaces` 是每个工作区各自的连接状态 |
| `POST` | `/multica/workspaces` | `{ token? }` → 这个令牌能访问的工作区（不带令牌就用已保存的）；无效 `422 invalid_multica_token`，没保存过 `422 multica_not_configured` |
| `PUT` | `/multica/settings` | `{ token?, workspaceIds }`：要监听的工作区，不带令牌就沿用已保存的、只改工作区（旧版 App 发的 `{ token, workspaceId }` 当作只有一个），先向 Multica 校验，再保存并重连；`422 invalid_multica_token` / `workspace_not_found` |
| `DELETE` | `/multica/settings` | 删除令牌并断开 |
| `POST` | `/multica/issues` | `{ issues: [{ workspaceId, issueId }] }`（最多 100 个）→ `{ issues }`：这些 issue 现在的项目、优先级、更新时间，桌面端「来电」页按它排序；删掉或无权访问的 issue 不返回；没设置令牌 `422 multica_not_configured` |

## 派单

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/multica/dispatch/options` | 主动派单能选的 `{ projects, agents }`：第一个监听的工作区里没完成、没取消的项目；每个 Agent 带 `online`（它的 Multica runtime 所在电脑在线），不在线的 Multica 不让派单。`POST` 同一路径带 `{ workspaceId }` 读别的监听工作区 |
| `POST` | `/multica/dispatch/issues` | `{ projectId, query?, statuses?, workspaceId? }` → `{ issues }`：派单时可以选的已有 issue——这个项目的 issue，按最近活动排序，最多 50 个（Multica `GET /api/issues?project_id=…&sort=last_activity`）；`query` 按标题里的词或编号（`61`、`OUTB-61`）过滤；`statuses`（状态 key 数组，如 `["backlog","in_progress"]`）只留处于其中任一状态的 issue（OUTB-61） |
| `POST` | `/multica/dispatch/statuses` | `{ workspaceId? }` → `{ statuses: [{ key, name }] }`：工作区的 issue 状态（含自定义状态，不含已归档的），按看板顺序（Multica `GET /api/issue-statuses`），App 用来做多选状态过滤 |
| `POST` | `/multica/uploads` | `{ name, type, data, workspaceId? }`（一张图片，base64）→ `{ attachment: { id, filename, markdownUrl } }`：派单的图片一张一张传，daemon 转给 Multica `POST /api/upload-file`（不带 issue）。张数不限、每张最大 100 MB，和 Multica 自己的限制一致（它的 `maxUploadSize`，网页端 `MAX_FILE_SIZE`，都没有张数上限；YOUT-226）；不是图片或超过 100 MB `400 invalid_dispatch`。请求体最大 140 MB |
| `POST` | `/multica/dispatches` | `{ projectId, agentId, prompt, attachments?, workspaceId? }` → `{ dispatch }`：在 `workspaceId`（不带就是第一个监听的工作区）调 Multica 智能创建（`POST /api/issues/quick-create`），由选中的 Agent 按用户说的话写 issue（标题、描述、优先级、截止日期都由它从原话里提取，默认指派给它自己），立即返回 `state: "creating"`。`attachments` 是上面传好的图片：把 `![文件名](markdownUrl)` 接在原话后面、id 放进 `attachment_ids`——和 Multica 网页的智能创建一样，Agent 把图片留在 issue 描述里，建 issue 时绑定到它上面；记录里 `prompt` 只存原话，`images` 存张数。Agent 不能运行 `422 agent_unavailable`（`message` 是 Multica 给的原因）、项目 / Agent 不存在 `422 project_not_found` / `agent_not_found`，原话和图片都没有、原话超过 8000 字 `400 invalid_dispatch`。带 `issueId`（不用 `projectId` / `agentId`）时不新建 issue，而是把原话和图片作为顶层评论发到这个 issue（`POST /api/issues/<id>/comments`，图片 id 放进 `attachment_ids`），由 Multica 照常触发它指派的 Agent；立即返回 `kind: "comment"`、`state: "created"`，`id` 是评论 id，`agentName` 是 issue 指派的 Agent（没有就是空）；issue 不存在 `422 issue_not_found`（OUTB-61） |
| `GET` | `/multica/dispatches` | `{ dispatches }`：从这台电脑派出的单（最多 100 条，新的在前，存在 `~/.outbrief/dispatches.json`），每次读都向 Multica 刷新：还在创建的看 Agent 的任务（任务关联上 issue → `created`；任务失败 / 取消 → `failed` / `cancelled`；任务完成 1 分钟后还没有 issue → `failed`），已创建的读 issue 现在的标题、状态、优先级 |
| `POST` | `/multica/dispatches/lookup` | `{ id }` → `{ dispatch }`：只刷新这一条（App 的「呼叫中」页轮询它）；`422 dispatch_not_found` |
| `POST` | `/multica/dispatches/cancel` | `{ id }` → `{ dispatch }`：还在创建时取消 Multica 任务，已经建好 issue 的不动 |

## 大模型和汇报语言

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/llm/settings` | `{ channel }`：生成简报用的接口，只有 `baseUrl`、`model`、`keyHint`、`structuredOutput`，从不返回 Key；没配是 `null` |
| `PUT` | `/llm/settings` | `{ baseUrl, apiKey?, model, structuredOutput? }`（`json_schema` 默认 / `json_object`）：保存并立即生效（不带 `apiKey` 沿用已保存的），返回 `{ channel }`；`422 invalid_llm_settings` / `llm_key_required` |
| `DELETE` | `/llm/settings` | 删掉，之后简报都是 `failed`；返回 `{ channel: null }` |
| `GET` | `/brief/language` | `{ language, source }`：简报的语言（BCP 47「语言-地区」，如 `zh-CN`、`es-MX`）；`source` 是 `app`（App 设的）或 `system`（还没设，按本机语言） |
| `PUT` | `/brief/language` | `{ language }`：保存并立即生效；`422 invalid_language` |

## 加密和配对（仅本机）

| 方法 | 路径 | 说明 |
|---|---|---|
| `GET` | `/e2e/key` | 仅本机：这台电脑的端到端密钥 `{ key, keyId, source, updatedAt }`，本机桌面端用它解密来电、加密回复 |
| `POST` | `/local/pairing` | 仅本机：`{ serverUrl, code, key, expiresAt, link }`。桌面 App 用它加入这台电脑所在的账号，并拿到这台电脑的端到端密钥（升级前的桌面 App 也靠它把旧的共享口令换成自己的设备令牌） |
| `PUT` | `/e2e/key` | 换密钥：`{ passphrase }`（至少 12 个字符，只保存由它算出的密钥）、`{ key: "obk1_…" }`（App 已经不用这种方式）或 `{ random: true }`；`422 passphrase_too_short` / `invalid_key` |

`OUTBRIEF_MULTICA_API_URL` 可以改 Multica 地址（默认 `https://api.multica.ai`）。

读 Multica（`GET`：issue、评论、项目、Agent、runtime、Agent 的任务）时，网络错误、5xx、429 最多重试 3 次（等 0.5 / 1 / 2 秒）；网络错误的日志带上原因（例如 `fetch failed (ECONNRESET)`）。写（发评论、上传派单图片、派单、取消派单）不重试，免得同一条回复发两次、同一个单派两次。

## 简报怎么生成

简报（来电里念给用户听的口播段落、卡片和待决问题）在 daemon 里生成：一次结构化 LLM 调用，然后检查每条 critical 事实都讲到了、口播不超过原文 60 %（原文 300 字以上时），不满足就重写一次，重写后仍漏掉的 critical 事实追加一段「补充说明」照读。

简报用哪种语言写（口播、卡片、结论、待决问题都是这种语言，不管汇报原文是什么语言）由 App「设置 → 语音 → 汇报语言」决定：youtube-dubbing-extension 目标语言里有 Azure 声音的 90 个 locale（如 `zh-CN`、`zh-TW`、`es-MX`），默认跟随系统。App 每次启动和切换时通过 `PUT /brief/language` 写进 `daemon.json` 的 `brief.language`，下一份简报立即生效；提示词里的语言名用 `Intl.DisplayNames` 取（如「墨西哥西班牙语」「中文（繁体，台湾）」），末尾「补充说明」段的标题也由大模型按这个语言给出。App 还没设过时按这台电脑的语言（macOS 的首选语言 `AppleLanguages`，再看 `LC_ALL` / `LANG` / `Intl`，补全成「语言-地区」），都不可用时用 `en-US`。

用哪个大模型在 App「设置 → 大模型」里设：选服务商、填 Key 和模型，保存后 App 通过`PUT /llm/settings` 写进本机 `~/.outbrief/daemon.json` 的 `llm.primary`，下一份简报立即生效，不用重启 daemon。只有这一个接口，没有备用通道（原来 ADR 0004 的主备切换和熔断已经去掉）：

```json
{
  "llm": {
    "primary": {
      "baseUrl": "https://api.deepseek.com",
      "apiKey": "<你的 key>",
      "model": "deepseek-flash",
      "structuredOutput": "json_object"
    }
  }
}
```

- 兼容 OpenAI 的接口都可以。简报是结构化 JSON，`structuredOutput` 决定怎么让模型按格式输出，在 App 里选，用「测试连接」检查：
  - `json_schema`（默认）：请求带 `response_format: json_schema`，由接口强制约束，最稳。
  - `json_object`：给不支持 json_schema 的模型用（DeepSeek、Claude 的 OpenAI 兼容接口、Ollama 本地模型等）。把 JSON Schema 写进系统提示词，请求带 `response_format: json_object`，用 AI SDK 的 `extractJsonMiddleware` 去掉模型可能包上的 markdown 代码块，最后用同一个 zod schema 校验，不符合就算这次调用失败。
  - 两种方式和模型不绑定：内置服务商只是带一个默认值，用户可以任意组合。
- `timeoutMs` 可选，默认 30000；`reasoningEffort` 只在写了时才发。这两项只能手工加，从 App 保存时接口地址和模型都没变就保留，换了就去掉（别家接口不一定认 `reasoning_effort`）。从 App 保存会整个替换 `llm`，手工留下的 `llm.fallback` 会被删掉（本来也不再读取）。
- 缺 `baseUrl` / `apiKey` / `model` 或格式不对视为没配。启动日志会写用的是哪个模型（不打印 key）。
- 调用失败（报错、非 2xx、超时、输出不符合 schema、被内容过滤）或者没配，汇报照样发给 server，只是简报是 `failed`（没配时 `llmCalls` 为 0，错误是「这台电脑没有配置大模型（在 App「设置 → 大模型」里填写）」）：来电时客户端直接读原文。
- 手工改 `daemon.json` 要重启 daemon 才生效。

## 发件箱（outbox）

生成简报要 5～60 秒，而 hook 只等 5 秒，所以汇报先落盘再慢慢处理：

1. hook 的 `POST /report`：只接受本机进程的请求（`Host` 必须是 `127.0.0.1` / `localhost` 加端口、不能带 `Origin`、`Content-Type` 必须是 `application/json`、请求体不超过约 0.8 MB），网页发来的请求一律拒绝，免得随便一个网页就能让用户手机响。然后校验、记下 `sessionId → cwd`、写入 `~/.outbrief/outbox.json`（先写临时文件再 rename，权限 600），然后立刻回 `202 {"accepted": true}`，这时简报还没有生成。Multica 任务完成后同样只是写入发件箱。
2. 后台最多同时生成 3 份简报（按到达顺序开始）。生成结果写回发件箱，之后发送失败或 daemon 重启都不会重新生成。
3. 连同简报 POST 给 server：`/v1/daemon/events`（本机 Agent）或 `/v1/daemon/multica-reports`（Multica）。网络错误、5xx、429 按指数退避重试（1 秒起，最长 5 分钟），跨重启继续；其他 4xx 记日志后丢弃；`409 duplicate_task` 表示别的电脑已经报过这个 Multica 任务，视为完成。
4. daemon 启动时恢复发件箱里所有未完成的汇报；退出时中断正在生成的简报，下次启动重新生成。

## Agent 完成回调（hook）细节

`outbrief-daemon hook <claude-code|codex>` 注册成 Claude Code 和 Codex 的 Stop hook，每轮 Agent 结束时把最终回复交给本机 daemon 的 `POST /report`（端口取 `~/.outbrief/daemon.json` 的 `localPort`，默认 8790）。原来单独的 `outbrief-hook` 仓库已并入这里。

- `outbrief-daemon install` 负责写配置：`~/.claude/settings.json` 和 `~/.codex/hooks.json` 的 `Stop`，命令是 `"<node>" "<本仓库>/src/cli.ts" hook claude-code|codex`。重复执行或换了目录再装，都会先删掉旧的 OutBrief hook（包括原 `outbrief-hook` 的），不会一轮响两次；`uninstall` 同样全部删掉。Codex 的 hook 命令变了之后要在 Codex 里重新信任一次。
- 两家的 Stop payload 都走 stdin，字段相同：`session_id`、`cwd`、`transcript_path`、`stop_hook_active`、`last_assistant_message`。Claude Code 老版本没有 `last_assistant_message` 时读 transcript。`stop_hook_active` 为真（hook 强制续跑后的再次 Stop）不上报。
- Codex 的 `session_id` 就是线程 id。只上报 `$CODEX_HOME/sessions`（默认 `~/.codex/sessions`）里有 rollout 文件的线程，也就是能被 `codex resume` 打开的线程；TUI 后台“生成任务标题”这类内部轮次会被跳过。
- 在 Multica 任务里（有 `MULTICA_ISSUE_ID` / `MULTICA_TASK_ID`）不上报：daemon 会从 Multica 读汇报，这里再报会来两次电。
- 5 秒超时，daemon 收下就回 `202`；daemon 没运行时汇报直接丢弃。任何失败都 `exit 0`，不影响 Agent。`cli.ts` 按子命令懒加载，hook 只依赖 Node 内置模块（路径和端口取自 `src/home.ts`，不经过 `config.ts`），所以 checkout 的 `node_modules` 被删掉（例如 Multica 清理已完成任务的工作目录）时 hook 照样能跑，有测试守着。

## 加密细节

汇报、简报、挂断后的回复、回复失败的原因都在这台电脑和用户的 App 之间加解密，server 只存、只转发密文（outbrief-server ADR 0007）：

- 密钥存在 `~/.outbrief/daemon.json` 的 `e2e.key`（`obk1_…`，权限 600）。第一次 `run` 时随机生成 32 字节；也可以在 App「设置 → 加密」里自己设一个密钥：用户填一句话（接口里叫 `passphrase`），用 PBKDF2-SHA256 60 万次算出 `obk1_` 密钥，只保存算出的密钥，不保存那句话。App 界面上只有这一个「密钥」输入框，算出的 `obk1_` 密钥和 `keyId` 都不显示。
- 同一台电脑上的桌面端通过下面的 `GET /e2e/key`（本机密钥鉴权）自动拿到密钥；别的设备扫这台电脑或已有设备显示的二维码时一起拿到，只输 6 位配对码时要输入同一个密钥（同一句话）。
- 算法：AES-256-GCM，每次随机 12 字节 IV，AAD 标明用途（汇报 / 某个事件的回复 / 某个回复的错误）。密文格式 `ob1.<keyId>.<iv>.<密文>`，`keyId` 用来区分「密钥不一致」和「数据损坏」，只在程序内部用。实现见 `src/e2e/crypto.ts`，App 的 `src/e2e/crypto.ts` 用 WebCrypto 实现同一格式，两边单测用同一组测试向量。
- 明文只留 server 路由要用的：来源（claude-code / codex / multica）、发生时间、Multica 任务 id（同一任务只来一次电）。
- 回复用别的密钥加密时，daemon 解不开，回一个不含内容的明文错误「无法解密回复……」，App 里能看到。
