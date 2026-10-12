---
title: 更新日志
description: OutBrief 各版本的变化。
---

版本号跟随 `outbrief-app` 的 [GitHub Releases](https://github.com/outbriefapp/outbrief-app/releases)。

## 0.0.1 · 2026-10-12

第一个公开安装包。

- **安装包**：macOS Apple Silicon（`.dmg`，Ad-hoc 签名）和 Android arm64（`.apk`，带来电推送）。
- **来电汇报**：Agent 完成任务后，账号下所有设备同时响铃；接听后语音 + 卡片播报，挂断时回复原会话或 Multica 评论。
- **daemon**：接 Claude Code / Codex 的 Stop hook 和 Multica 工作区，用本机配置的大模型生成简报。
- **server**：只转发密文的中转服务，匿名账号 + 设备配对，Android 来电推送唤醒（[ADR 0010](/docs/adr/0010-wake-phones-with-push/)）。
