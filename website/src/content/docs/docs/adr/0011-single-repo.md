---
title: "ADR 0011：合并成一个仓库"
sidebar:
  label: "0011 合并成一个仓库"
---

- 状态：已采纳（2026-10-12，OUTB-74）
- 决策者：zhibo li
- 取代：ADR 0003 第 1 条（三个独立仓库）

## 背景

开源后代码分在四个仓库：`outbrief-app`、`outbrief-server`、`outbrief-daemon`、`outbrief-website`。对外没有一个总入口，发给用户时要么给好几个地址，要么只能给官网；star 和 issue 会分散在各个仓库，以后拉赞助也不知道该指向哪个仓库。

这几个仓库本来就是一起改的：接口在 server、app、daemon 各有一份镜像类型，改一次接口要在三个仓库各提一个 PR，再协调合并顺序；App 的 `pnpm dev:all` 要按目录名去猜同级的 server 在哪。

## 决策

1. **所有代码放进一个仓库 `outbriefapp/outbrief`**，由原 `outbrief-app` 改名而来，保留它的 star、tag 和 Releases；旧地址由 GitHub 自动跳转。
2. 目录：`app/`（客户端）、`server/`、`daemon/`、`website/`（官网和文档站）。另外三个仓库的提交历史用 `git filter-repo` 改写到各自的子目录后合并进来，`git log` 能查到原来的提交。
3. **每个目录仍然是独立的 pnpm 项目**，有自己的 `package.json` 和 `pnpm-lock.yaml`，不引入 pnpm workspace；接口镜像类型的做法（ADR 0003 第 2 条）不变。
4. CI 按目录拆成 `app`、`server`、`daemon`、`website` 四个 workflow，只有改到对应目录才运行。
5. 部署跟着目录走：Railway 的 server 服务 Root Directory 设为 `/server`，配置文件路径设为 `/server/railway.json`，只有 `/server/**` 有改动时才部署；Cloudflare Pages 的 Root directory 设为 `website`，构建监听路径 `website/*`。
6. 旧的 `outbrief-server`、`outbrief-daemon`、`outbrief-website` 在 README 里注明已迁移，然后归档（只读保留，不删除）。
7. `LICENSE` 和 `NOTICE` 只在仓库根目录放一份。

## 理由

- 一个地址就能找到全部代码和文档，star、issue、Discussions、赞助都集中在一处。
- 跨组件的改动可以放在一个 PR 里，一起评审、一起合并。
- 现在还在早期（star 和外链都很少），迁移成本最低。

## 放弃的方案

- **只建一个放 README 的入口仓库，代码仓库不动**：用户看代码时还是会在代码仓库点 star、提 issue，起不到集中的作用。
- **引入 pnpm workspace 共享协议包**：ADR 0003 已经说明接口面很小，不值得为此改构建方式；以后需要时可以在这个仓库里再加。
