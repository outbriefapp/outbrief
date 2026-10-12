---
title: "快速开始"
description: "部署 server、安装 daemon 和 App，并把设备配对到同一个账号。"
---

来电只在同一个匿名账号里转发。推荐让跑 Agent 的那台电脑上的 daemon 创建账号，桌面 App 和手机 App 都加入这个账号。已经有服务器时（例如 `https://api.outbriefapp.com`），从第 2 步开始，daemon 和 App 都填这个地址。注册关闭时不能自己建账号，用主人给出的配对码加入。

1. **部署 [outbrief-server](/docs/server/)**。只跑一个进程。私有化部署（默认）在还没有主人时，每次启动的日志里打印一次性认领码 `Claim code: XXXX-XXXX-XXXX`。记下服务地址。手机和别的电脑要能访问这个地址；`http://127.0.0.1:8787` 只有部署 server 的那台机器自己能用。给手机配对时，daemon 和 App 里填局域网 IP 或公网 `https://` 地址。命令见 [Server 自部署](/docs/server/#部署)。
2. **在跑 Agent 的电脑上安装 [outbrief-daemon](/docs/daemon/)**。`pnpm install` 之后执行 `node src/cli.ts login --server <服务地址>`，输入认领码。终端打出二维码和 6 位配对码。macOS 上接着执行 `node src/cli.ts install`（开机自启，并写入 Claude Code / Codex 的 Stop hook）。`install` 把 node 和 `src/cli.ts` 的绝对路径写进 launchd，仓库留在原地。
3. **在同一台电脑上安装桌面 App（[outbrief-app](/docs/app/)）**。`pnpm tauri build`，安装包在 `src-tauri/target/release/bundle/`。开发时用 `pnpm tauri dev`。daemon 已经在运行时，桌面端第一次打开会自动加入这台 daemon 的账号。
4. **安装手机 App（[outbrief-app](/docs/app/)，和桌面 App 同一个仓库）**。可安装的 Android 包在那个仓库里执行 `scripts/build-android.sh`，产物是 `dist-android/OutBrief.apk`。来电推送由这个脚本打进包里。开发调试用 `pnpm tauri android dev` 或 `pnpm tauri android build` 时，要先 `export OUTBRIEF_FIREBASE_CONFIG=<google-services.json>`，否则包里没有推送。指向 `https://api.outbriefapp.com` 的安装包，必须用那台服务器所属的 Firebase 项目打包；自己另建的项目收不到它的推送，这个项目的 `google-services.json` 不在仓库里。iOS 用 `pnpm tauri ios init`，然后 `pnpm tauri ios dev` 或 `pnpm tauri ios build`；这一版没有来电推送。需要 [Tauri 的移动端环境](https://tauri.app/start/prerequisites/)。仓库里没有应用商店安装包。装好后按 App 文档的[「手机在后台或锁屏时响铃」](/docs/app/#手机在后台或锁屏时响铃)打开后台来电、通知、锁屏来电界面、来电弹出界面，电池选不受限制，并确认「推送唤醒」是已开启。配对：已配对设备打开「设置 → 设备 → 添加设备」，或在跑 daemon 的电脑上执行 `node src/cli.ts pair`，用手机摄像头扫二维码。

### 配对

没有登录，也没有共享口令。谁先装谁建账号，后来的设备用 6 位配对码加入。配对码 10 分钟有效、只能用一次。二维码和配对链接是 `outbrief://pair?server=<服务地址>&code=<6位>&key=obk1_…`。服务地址和端到端密钥由设备直接交给设备，server 看不到密钥。

| 已在账号里 | 要加入的设备 | 怎么做 |
|---|---|---|
| 这台电脑的 daemon 正在运行 | 同一台电脑的桌面 App | 自动。App 用本机 `~/.outbrief/local-api.key` 向 `127.0.0.1:8790` 要配对码和密钥 |
| 桌面 App，或 daemon（`node src/cli.ts pair`） | 手机 App | 手机扫「设置 → 设备 → 添加设备」或终端里的二维码 |
| 手机或另一台电脑上的 App | 一台电脑的 daemon | 在「添加设备」页复制命令，在那台电脑上执行 `node src/cli.ts login 'outbrief://pair?…'` |
| 任意已配对设备 | 另一台电脑的桌面 App | 桌面端把配对链接贴进欢迎页。桌面端不开摄像头 |
| 只拿到 6 位数字 | daemon 或 App | 再输入「设置 → 加密」里的同一句话（至少 12 个字符）。没设过这句话时，用带 `key=` 的二维码或配对链接 |

先打开 App、由 App 创建账号也可以：欢迎页填服务地址和认领码，点「创建新账号」，再用「添加设备」里的链接在电脑上 `login`。同一台电脑上的 daemon 配对并运行之后，这台电脑的桌面 App 加入的是 daemon 所在的账号。

只有手机、不装桌面 App：做完第 1、2 步，用手机扫 daemon 登录时终端里的二维码。手机上的 Multica、大模型、汇报语言经 server 加密转给这台电脑的 daemon，电脑要在线。

公共云端把 `OUTBRIEF_OPEN_SIGNUP=true` 打开后，第一台设备直接建账号，认领码不用填。
