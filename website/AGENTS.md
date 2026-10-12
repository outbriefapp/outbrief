# 给 Agent 的规则

- 中文是默认语言，页面放在 `src/content/docs/`；英文版放在 `src/content/docs/en/` 的同名路径。改了一边的内容，另一边一起改；ADR 只有中文。
- 站内链接写绝对路径并带结尾斜杠（`/docs/app/`、`/en/docs/app/`）。改完跑 `pnpm build`，确认没有新的警告。
- 不要把 token、账号、口令、私有地址写进页面、配置或 PR。
- 本地起站点：`pnpm dev`（默认 `http://localhost:4321`）。搜索（Pagefind）只在 `pnpm build && pnpm preview` 下可用。
