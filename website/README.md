# outbrief-website

OutBrief 官网和文档站：<https://outbriefapp.com>。

用 [Astro](https://astro.build) + [Starlight](https://starlight.astro.build) 构建，部署在 Cloudflare Pages（GitHub 集成：`main` 发布生产，每个 PR 自动生成预览地址）。

## 本地开发

需要 Node ≥ 22.18（见 `.nvmrc`）、pnpm 9。

```bash
pnpm install
pnpm dev        # http://localhost:4321
pnpm build      # 输出到 dist/，同时生成 Pagefind 搜索索引
pnpm preview    # 预览 dist/，搜索只在 build 之后可用
```

## 目录

| 路径 | 内容 |
|---|---|
| `src/content/docs/` | 中文页面（默认语言，挂在根路径） |
| `src/content/docs/en/` | 英文页面，挂在 `/en/`。缺英文版的页面（ADR）回退显示中文 |
| `src/content/docs/docs/` | 文档：快速开始、App、Daemon、Server 自部署、ADR |
| `src/components/PromoVideo.astro` | 首页宣传视频 |
| `public/media/` | 宣传视频和封面图（单个文件不能超过 Cloudflare Pages 的 25 MiB 上限） |
| `public/proposal.html` | 最初的方案汇报 |

三个代码仓库（[outbrief-server](https://github.com/outbriefapp/outbrief-server)、[outbrief-app](https://github.com/outbriefapp/outbrief-app)、[outbrief-daemon](https://github.com/outbriefapp/outbrief-daemon)）的使用文档和 ADR 都在这里维护，仓库 README 只留简介和官网链接。改了接口、命令或安装步骤时，在这里同步改文档。

## 常用命令

| 命令 | 作用 |
|---|---|
| `pnpm lint` / `pnpm format` | Biome 检查 / 自动修复 |
| `pnpm check` | `astro check`（类型和内容 schema） |
| `pnpm build` | 构建静态站点 |

## Cloudflare Pages 配置

| 项 | 值 |
|---|---|
| 构建命令 | `pnpm build` |
| 输出目录 | `dist` |
| 环境变量 | `NODE_VERSION=22`（Node 版本也可由 `.nvmrc` 决定） |
| 生产分支 | `main` |
| 自定义域名 | `outbriefapp.com`；`www.outbriefapp.com` 301 跳转到 apex |

## License

[OutBrief License](LICENSE)（基于 Apache License 2.0 并附加条件）。
