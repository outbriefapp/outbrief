---
title: "App 源码目录"
description: "outbrief-app 的源码结构：每个模块负责什么。"
---

:::note
这一页给想改 App 代码的开发者看。功能怎么用，见「使用指南」。
:::

## 来电和通话

| 路径 | 内容 |
|---|---|
| `src/callQueue.ts` | 来电 / 排队 / 挂断状态机：汇报先「准备中」，整份语音生成好才响铃；生成失败的不响铃，在空闲页可重试 / 忽略。排队的汇报按来电页的顺序响铃（先 issue 优先级，再更新时间），还没上屏的队首会让给更紧急的汇报；没上屏的汇报都是「未接来电」，可以在来电页任选一个直接接听。不在响铃时段收到的汇报（包括 App 没开时在不响铃时段里收到的）、收到超过 2 分钟这台设备才知道的汇报（App 关着、断网，或更新安装包 / 重启前就在等的来电，OUTB-60）、以及进入不响铃时段时还没接的汇报，记为未接，之后回到响铃时段也不会自己响铃，只能在来电页接听或「全部知悉」。账号下所有设备同时响铃，像 FaceTime / 微信多端来电：一台接听、拒绝或「全部知悉」，别的设备马上停止响铃、从未接来电里拿掉，历史里记「已在 iPhone 接听」；两台几乎同时点接听只算先到服务端的那台，晚的那台通话结束；断线期间在别的设备上结束的来电，重连后也拿掉（OUTB-57）（纯函数，有单测） |
| `src/callModes.ts`、`src/useRingSchedule.ts` | 模式：名字 + 时段 + 重复（周几用），规则是「只在这些时段响铃」或「这些时段不响铃」（时段可跨夜，如 22:00–08:00）。可以同时打开多个模式，只要重复不在同一天；每天按当天的模式算（0:00–24:00），没有模式的日子随时响铃；算出现在能不能响铃、什么时候变（最多往后看一周，有单测） |
| `src/components/ModesPage.tsx` | 设置 → 模式，照着闹钟做：列表每行大字显示时段，下面写「名称 · 重复 · 规则」，右边一个开关，开关立即生效。重复不在同一天的模式可以一起打开（例如工作日一个、周末一个）；打开一个模式会关掉和它同一天的模式，并提示关了哪个。右上角「+」新建，点一行进编辑页：开始 / 结束时间用滚轮选（`react-mobile-picker`，可滚动、拖动或点数字），可以加多个时段；「重复」点选周一到周日（默认每天）；规则是「时段内响铃 / 时段内不响铃」；再填名称，「存储」保存，底部「删除模式」。首次安装带「工作」（工作日 10:00–19:00 响铃）和「睡眠」（每天 22:00–08:00 不响铃），都不打开，即「随时响铃」。旧版本存的模式按每天用，原来选中的模式保持打开 |
| `src/ringtones.ts`、`src/ringtoneStore.ts`、`src/useRingtone.ts`、`src/components/RingtonesPage.tsx` | 设置 → 通话 → 铃声（YOUT-228），照着手机的「电话铃声」做成两层：第一层只有「来电铃声」「呼叫等待铃声」两行，右边是现在用的铃声；点进去是这一项自己的页面。来电铃声默认「经典」（原来的铃声）；呼叫等待铃声在呼叫 Agent 后「呼叫中」一直放，直到 issue 建好、失败或取消，默认「回铃音」（450 Hz 响 1 秒停 4 秒），也可以「不响」。每一项的页面：顶部固定显示当前铃声（可试听），铃声超过 8 个出现搜索框；「我的铃声」第一行是「添加铃声…」（任意音频文件，可以一次选多个，每个最大 10 MB，音频存在这台设备的 IndexedDB `outbrief-ringtones`），在哪一项添加就直接用作哪一项；新加的在前，只显示前 5 个，其余点「显示全部」；被另一项在用的标「来电铃声在用 / 等待铃声在用」；下面是 6 个内置铃声（`public/ringtones/`）。点一个会试听一遍并选中。右上角「编辑」可以直接改名、勾选多个一起删；删正在用的会先确认，那一项回到默认（有单测） |
| `src/call/prepare.ts` | 来电前在客户端把简报的每一句都合成好，放进语音缓存；通话中直接从缓存播放（有单测） |
| `src/call/session.ts` | 通话中状态机：分段 / 逐句播放位置、打断、跳页、问答、决策、挂断（纯函数，有单测） |
| `src/call/useCall.ts`、`src/call/player.ts` | 通话副作用：逐句合成 + 预取播放、流式回答边收边读 |
| `src/call/resume.ts` | 回到原 Agent 会话的命令（`claude --resume` / `codex resume`，有单测） |
| `src/call/answered.ts` | 一通电话在一台设备上只响一次（YOUT-226）：服务端在通话结束前一直把它当待接，App 重启或页面刷新后会再推一次。已经在本机结束的（历史里有）不再出现，把结果重新报给服务端（上次可能没报上）；接听过、通话被重启打断的放进未接来电，不再响铃。接听过的 id 记在 `outbrief.answered`（最多 200 个，有单测） |
| `src/call/records.ts` | 本机历史记录 `outbrief.call.<eventId>`（最多 100 条，存储满了先删最旧的）：来电结束时把整个事件（汇报、简报）连同通话对话、决策、发出的回复存下来；之后收到的投递状态只合并状态字段。服务端来电结束就擦掉汇报和简报，不保存历史（有单测） |
| `src/callList.ts`、`src/components/HistoryScreen.tsx` | 「来电」页：上面是未接来电（在排队、还没接的汇报，例如睡眠模式里攒下的），每行点「接听」直接接通，不用按顺序一个个来；下面是本机通话记录。未接来电标题右边有「全部知悉」：当前页签里的未接来电都知道了、不再接听，一起记为「已知悉」进通话记录（服务端状态 `acknowledged`）。每一行都写来电时间，精确到秒（`2026-09-29 14:05:33`），鼠标悬停看 issue 更新时间。按项目分页签（只有 Multica issue 所属的项目才有页签；本机 Agent 的汇报、不在项目里的 issue 不算项目，目录名、汇报标题都不当项目名），外加「全部」：「全部」里两个列表都按项目分组、组名只写项目名，不属于任何项目的来电放在最后的「其他」组里。每个列表先按 issue 优先级（紧急 → 高 → 中 → 低 → 无），同优先级按 issue 更新时间倒序。打开时经本机 daemon（`POST /multica/issues`）从 Multica 刷新通话记录的优先级、更新时间和项目并存回记录（有单测） |
| `src/components/ProjectTabs.tsx`、`src/tabsFit.ts` | 项目页签只占一行：触屏上用手指左右滑；用鼠标时放不下的页签收进行尾的「更多」菜单（有未接来电时显示红色数字），当前页签始终留在行里（`fitTabs` 有单测） |

## 语音和大模型

| 路径 | 内容 |
|---|---|
| `src/llm/providers.ts`、`src/llm/endpoint.ts`、`src/components/LlmSettingsSection.tsx` | 设置 → 大模型：服务商预设、接口地址规整、`GET /models` 取模型列表、测试连接（结构化输出，和 daemon 生成简报同一种调用）；保存时同步给本机 daemon（有单测） |
| `src/llm/qa.ts` | 通话中提问：客户端用 AI SDK（`ai` + `@ai-sdk/openai-compatible`）直接流式请求用户自己的兼容 OpenAI 接口，提示词带上汇报全文（不截断）、简报、已听进度和对话历史（有单测）。Tauri 里走 `@tauri-apps/plugin-http` 的 fetch（接口不给 webview 发 CORS 头），浏览器 `pnpm dev` 里用普通 fetch |
| `src/voice/` | TTS / STT、录音、分句（见该目录）。语音只从本机直接请求用户选的语音平台（`tts/`），没有服务端兜底：服务端不能看到简报的明文 |
| `src/voice/tts/` | 语音平台（设置 → 语音）：`types.ts` 是引擎接口，`registry.ts` 是注册表（加一个平台 = `engines/` 下加一个模块 + 注册表加一行，其他代码不认识具体平台），`engines/` 是 12 个内置平台，`template.ts` 是自定义 HTTP 请求模板、`curl.ts` 解析 cURL 并自动认出句子 / 音色 / 语速的位置、`audio.ts` 解析各家的返回（裸音频 / JSON 里的 base64、hex、URL / 逐行 JSON / 裸 PCM 加 WAV 头），`settings.ts` 存每个平台各自的配置（均有单测） |
| `src/voice/languages.ts`、`src/voice/voices.ts`、`src/voice/azureVoices.ts`、`src/voice/phrases.ts`、`src/useSpeechLanguage.ts` | 设置 → 语音的**汇报语言**：语速（0.5×–2×）由各平台的接口范围再夹一次，接口没有语速参数的平台（Gemini、Qwen-TTS、ChatTTS、CosyVoice）会在页面上说明语速对它无效。汇报语言照搬 youtube-dubbing-extension 插件端的目标语言：它的语言表里有 Azure 声音的 90 个 locale（名称也用插件的，如「美国(英语)」「中国台湾」），默认跟随系统（系统首选语言按「语言-地区」匹配，没有就取该语言最常用的地区，如 `pt` → `pt-BR`，都没有用 `en-US`）。`azureVoices.ts` 由插件的 `AzureVoiceMap` / `VoiceCodeLabelMap` 生成，每个语言的声音和插件一样，按女声 / 男声分组（去掉 path B 返回 400 的 DragonHD 声音和已下线的晓萱），当前语言的声音显示本语言名、其他语言的多语言声音显示英文名；多语言声音用于别的 locale 时 SSML 加 `<lang xml:lang>`（和插件的 EdgeTtsSsmlBuilder 一样）。本机 daemon 用汇报语言写简报（App 启动和切换时 `PUT http://127.0.0.1:8790/brief/language`，daemon 没运行就每 30 秒重试，被拒绝（例如密钥不一致）不重试；没有本机 daemon 时经服务端加密转发给「设置 → 大模型 / Multica」里选的电脑），来电念的内容、没有简报时那句提示和试听（`phrases.ts`，74 种语言）、通话中的回答也都用它。存的声音不属于当前语言时用该语言的第一个女声，切回原来的语言时恢复（有单测） |

## 账号、设备和加密

| 路径 | 内容 |
|---|---|
| `src/sse.ts`、`src/serverClient.ts` | SSE 订阅（`Last-Event-ID` 续传）、回复、状态回写，以及账号 / 设备 / 配对码接口。设备令牌被拒（401，这台设备在别处被移除了）时停止重连，回到欢迎页 |
| `src/account.ts`、`src/pairing.ts`、`src/components/WelcomeScreen.tsx`、`src/components/DevicesPage.tsx`、`src/components/JoinForm.tsx` | 匿名账号和设备配对（outbrief-server ADR 0008），见下文「账号和设备」（有单测） |
| `src/daemonLink.ts`、`src/useDaemon.ts` | 改 outbrief-daemon 设置（Multica、简报大模型、汇报语言）的两条路：本机 daemon（`127.0.0.1:8790`，用 `~/.outbrief/local-api.key` 鉴权，Tauri 的 `read_daemon_local_key` 读文件）；没有本机 daemon 时（手机）用端到端密钥加密后经服务端 `POST /v1/devices/<电脑>/settings` 转给账号下的电脑（有单测） |
| `src/settings.ts` | 本地设置 `outbrief.settings`：服务地址和这台设备的账号（设备令牌 `oba_…`、账号 id、设备 id；旧版本存的共享口令会被丢弃）、汇报语言、声音类型、声音、语速、称呼、大模型（接口地址 / API Key / 模型）、端到端密钥（`e2eKey`，以及是否跟随本机 daemon），全部只存在本机。没有登录 |
| `src/protocol.ts` | `outbrief-server` `src/protocol.ts` 的镜像类型，改接口时一起改。服务端转发的是密文 `RelayedEvent`，App 里用的是解密后的 `AgentEvent` |
| `src/e2e/` | 端到端加密（outbrief-server ADR 0007）：`crypto.ts` 用 WebCrypto 实现 AES-256-GCM，以及从用户设的密钥（一句话）派生出真正的 `obk1_` 密钥，和 outbrief-daemon 用同一组测试向量；`events.ts` 解密来电和失败原因、加密回复；`useE2eKey.ts` 有本机 daemon 时跟随它的密钥（`GET http://127.0.0.1:8790/e2e/key`，本机密钥文件鉴权）（均有单测） |
| `src/components/E2eSettingsSection.tsx` | 设置 → 加密：只有一句提示（在这里可以换一个新的密钥，请确保和电脑端的密钥保持一致）和一个「密钥」输入框：用户填一句至少 12 个字符的话，App 用它算出真正的 `obk1_…` 密钥（`keyFromPassphrase`），本机 daemon 在线时一起换掉。算出来的密钥和它的 `keyId` 都是内部细节，界面上不显示 |

## 界面和设置

| 路径 | 内容 |
|---|---|
| `src/components/` | 空闲 / 来电 / 通话中 / 挂断回复 / 来电列表（只读本机记录）/ 设置界面 |
| `src/i18n/` | 界面语言（设置 → 界面语言）：跟随系统（系统首选语言是中文就用中文，否则英文）/ 中文 / English。`zh.ts`、`en.ts` 是两套完整文案，类型保证 key 一致；中文下产品名是「启奏」，英文下仍是「OutBrief」。组件用 `useT()`（切换语言立即重绘），组件外的报错、描述文字用 `t()`。窗口标题、托盘提示、托盘菜单和 macOS 菜单栏（编辑 / 窗口等）由 `src/shell.ts` 调 Rust 的 `localize_shell` 跟着改。菜单栏最左边的粗体 App 名是 macOS 在启动时从 bundle 的 `CFBundleName` 读的，运行中改不了：`localize_shell` 把当前语言的名字记进 App 的 user defaults（`OutBriefAppName`），下次启动前写进 bundle，所以切换语言后重开一次 App 才变成「启奏」/「OutBrief」。打包后的 App 另有 `src-tauri/macos/<语言>.lproj/InfoPlist.strings`，Dock、访达里的名字跟随系统语言。只改界面文字：简报、语音、通话中的回答的语言在「设置 → 语音 → 汇报语言」里设（有单测） |
| `src/components/DispatchScreen.tsx`、`src/dispatch.ts`、`src/useDispatchOptions.ts` | 呼叫 Agent（主动派单，YOUT-222）：空闲页正中间状态文字下面的绿色圆形电话按钮进入（这一页唯一要主动做的事，所以放在正中间，不和右上角的「来电」挤在一起、也不再是两个长得很像的电话图标），一页直发：项目和 Agent 默认是这台设备上一次派单用的（`outbrief.dispatch.last`），要换就点开底部面板；在文本框里用手机输入法的语音说需求（优先级、截止日期也一起说），可以附图片（文本框下面的图片按钮选，桌面端也能直接粘贴截图或拖进文本框）：和 Multica 自己的限制一致，张数不限、每张最大 100 MB（Multica 网页端 `MAX_FILE_SIZE` / 服务端 `maxUploadSize`）；超过 1.5 MB、长边超过 2048 或不是 PNG / JPEG / WebP / GIF 的先在本机转成长边 2048 的 JPEG；发送时经 daemon 一张一张传到 Multica（`POST /multica/uploads`），再派单（`src/dispatchImages.ts`，YOUT-226），只发图片也行，「呼叫并派单」后经电脑上的 daemon（`POST /multica/dispatches`）调 Multica 智能创建，由选中的 Agent 写 issue（标题、优先级、截止日期都由它从原话里提取；图片由 daemon 先传到 Multica，放进 issue 描述），「呼叫中」每 3 秒问一次 issue 建好没有，建好后「已派单」显示编号和标题；可以取消，或不等了先去「我的派单」。「我的派单」列出这台电脑派出的所有单（记在电脑上，同一账号的设备都能看到）和 issue 现在的状态。项目、Agent 每 10 秒经 daemon 重读一次：电脑离线、本机 daemon 没运行、电脑上没设 Multica、选中的 Agent 所在电脑在 Multica 里不在线时，按钮禁用并写明原因，恢复后自动能派（`src/dispatch.ts` 有单测） |
| `src/components/SettingsForm.tsx` | 设置首页是分组列表（通用 / 连接 / 个人 / 集成 / 通话），每一行点进去是单独的一页，各自保存；以后加设置项就加一行 |
| `src/components/MulticaSettingsSection.tsx` | 设置 → Multica：填自己的 Multica API Token → 验证 → 勾选要监听的工作区（可多选）→ 保存；之后点「更改工作区」不用再填令牌。监听多个工作区时，来电在项目名前标出工作区，主动派单页多一行「工作区」可选（默认上次派单的，没有就是第一个）。首次安装为空，不带任何默认令牌。令牌只存在电脑上的 outbrief-daemon（桌面端直接请求本机 `http://127.0.0.1:8790/multica/*`，用本机密钥文件鉴权；手机经服务端加密转发给账号下的电脑，服务端读不到），不以明文经过服务端；之后只显示 `mul_…xxxx`。daemon 用它监听 Multica 任务、发挂断后的回复，电脑关机时 Multica 任务不会来电 |
| `src/addressName.ts` | 设置 → 称呼（默认「老板」）：简报里用 `{称呼}` 占位，客户端收到来电 / 读历史时替换成自己的称呼；通话中提问时也按这个称呼回答 |

## 桌面壳

| 路径 | 内容 |
|---|---|
| `src-tauri/` | Rust 壳：托盘（显示窗口 / 退出，关窗口只隐藏）；`tauri-plugin-http` 供通话中提问请求大模型，`capabilities/default.json` 放行任意 `http://*:*` / `https://*:*`（接口地址由用户填，端口不固定） |

