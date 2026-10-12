# 给 Agent 的规则

- 中文是默认语言，页面放在 `src/content/docs/`；英文版放在 `src/content/docs/en/` 的同名路径。改了一边的内容，另一边一起改；`docs/reference/` 和 ADR 只有中文。
- 站内链接写绝对路径并带结尾斜杠（`/docs/app/`、`/en/docs/app/`）。改完跑 `pnpm build`，确认没有新的警告。
- 不要把 token、账号、口令、私有地址写进页面、配置或 PR。
- 本地起站点：`pnpm dev`（默认 `http://localhost:4321`）。搜索（Pagefind）只在 `pnpm build && pnpm preview` 下可用。
- 文档按读者分层：`guides/` 写给使用者，说明怎么做、会看到什么，不讲源码；接口、源码、内部机制放进 `reference/`。步骤用 `<Steps>`，平台差异用 `<Tabs>`，提醒用 `<Aside>`，界面截图用 `Screen` 组件。
- 页面里写的菜单名、按钮名以 outbrief-app 的 `src/i18n/zh.ts` / `en.ts` 为准。
