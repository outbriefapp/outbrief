# OutBrief

[English](README.en.md)

**AI Agent 干完活，会给你打电话。** OutBrief 把 AI Agent 的工作汇报变成一通视频来电：接起来听语音简报、看汇报卡片，随时打字打断，挂断时说一句，回复就回到原来的会话。

- 官网和文档：**<https://outbriefapp.com>**
- [下载安装包](https://github.com/outbriefapp/outbrief/releases/latest)
- [快速开始（安装顺序与设备配对）](https://outbriefapp.com/docs/quick-start/)
- 问题、反馈和建议：[Discord](https://discord.gg/TG8qHzdv6)，或者在这里提 [Issue](https://github.com/outbriefapp/outbrief/issues)

觉得有用的话，欢迎点个 ⭐ Star。

## 仓库结构

| 目录 | 内容 | 文档 |
|---|---|---|
| [`app/`](app/) | 客户端：Tauri 2 + React 19，桌面 / Android，负责响铃、语音和卡片播报 | [App](https://outbriefapp.com/docs/app/) |
| [`server/`](server/) | 服务端：Hono + MySQL，只转发密文，推送来电 | [自部署 Server](https://outbriefapp.com/docs/server/) |
| [`daemon/`](daemon/) | 每台跑 Agent 的电脑上的常驻进程：接收 Claude Code / Codex / Multica 的汇报，生成简报，挂断后送回回复 | [Daemon](https://outbriefapp.com/docs/daemon/) |
| [`website/`](website/) | 官网和文档站：Astro + Starlight | [本地开发](https://outbriefapp.com/docs/development/) |

每个目录是一个独立的 pnpm 项目，在各自目录里 `pnpm install`、`pnpm test`。本地开发见 [开发文档](https://outbriefapp.com/docs/development/)，架构决策见 [ADR](https://outbriefapp.com/docs/adr/)。

## License

[OutBrief License](LICENSE)（基于 Apache License 2.0 并附加条件，参照 [Multica License](https://github.com/multica-ai/multica/blob/main/LICENSE)）。
