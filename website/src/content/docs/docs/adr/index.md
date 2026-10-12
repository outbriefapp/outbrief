---
title: 架构决策记录（ADR）
description: OutBrief 的架构决策，以及每个决策为什么这样做。
sidebar:
  label: 概览
  order: 0
---

每份 ADR 记一个架构决策：背景、选了什么、为什么、放弃了什么。被后来的 ADR 修订或取代时，在开头的「状态」里注明。

最初的方案见 [方案汇报](/proposal.html)。

| 编号 | 决策 |
|---|---|
| [0001](/docs/adr/0001-stack/) | 技术栈与分层 |
| [0002](/docs/adr/0002-llm-voice-placement/) | LLM 调用方式与语音链路放置 |
| [0003](/docs/adr/0003-repos-mysql-azure-stt/) | 拆成三个仓库、存储改 MySQL、STT 首期走 Azure（仓库部分已被 0011 取代） |
| [0004](/docs/adr/0004-llm-gemini-flash-failover/) | LLM 用 Gemini 3.8 Flash，CLIProxy 主通道 + Vercel AI Gateway 兜底 |
| [0005](/docs/adr/0005-llm-on-user-devices/) | 大模型调用全部放到用户设备上 |
| [0006](/docs/adr/0006-relay-without-login-or-history/) | 服务端只做中转——去掉登录，历史记录放到用户设备上 |
| [0007](/docs/adr/0007-end-to-end-encryption/) | 端到端加密——服务端只转发密文 |
| [0008](/docs/adr/0008-anonymous-accounts-and-device-pairing/) | 匿名账号 + 设备配对，去掉共享口令 |
| [0009](/docs/adr/0009-single-instance-first/) | 先单实例上线，有压力再引入 Redis 做多实例 |
| [0010](/docs/adr/0010-wake-phones-with-push/) | 用推送叫醒手机接来电 |
| [0011](/docs/adr/0011-single-repo/) | 合并成一个仓库 |
