# 给 Agent 的规则

这是 OutBrief 的 monorepo：`app/`（客户端）、`server/`、`daemon/`、`website/`（官网和文档站）。每个目录是独立的 pnpm 项目，命令在对应目录里跑；各目录的 `AGENTS.md` 有该组件自己的规则。

- 自测不能给用户打电话：本机正在运行的 server（`localhost:8787`）和 daemon（`127.0.0.1:8790`）是用户本人在用的环境，细则见 `server/AGENTS.md`、`daemon/AGENTS.md`。
- 改了接口：`server/src/protocol.ts` 为准，`app/src/protocol.ts`、`daemon/` 里的镜像类型一起改，放在同一个 PR 里。
- 改了接口、命令或安装步骤：同一个 PR 里同步改 `website/` 的文档。
- 不要把 token、账号、口令、私有地址写进代码、配置、文档或 PR。
