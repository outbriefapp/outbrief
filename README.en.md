# OutBrief

[中文](README.md)

**Your AI agent calls you when it's done.** OutBrief turns an agent's report into a video call: pick up for a spoken brief with cards, type to cut in, and say your reply as you hang up; it goes back to the same session.

- Website and docs: **<https://outbriefapp.com/en/>**
- [Download](https://github.com/outbriefapp/outbrief/releases/latest)
- [Quick start (install order and pairing)](https://outbriefapp.com/en/docs/quick-start/)
- Questions, feedback and ideas: [Discord](https://discord.gg/TG8qHzdv6), or open an [issue](https://github.com/outbriefapp/outbrief/issues) here

If you find it useful, a ⭐ star helps.

## Repository layout

| Folder | What it is | Docs |
|---|---|---|
| [`app/`](app/) | Client: Tauri 2 + React 19, desktop and Android; rings, speaks and shows the cards | [App](https://outbriefapp.com/en/docs/app/) |
| [`server/`](server/) | Server: Hono + MySQL; relays ciphertext only and pushes calls | [Self-host the server](https://outbriefapp.com/en/docs/server/) |
| [`daemon/`](daemon/) | Resident process on each computer that runs agents: takes Claude Code / Codex / Multica reports, writes the brief, delivers replies | [Daemon](https://outbriefapp.com/en/docs/daemon/) |
| [`website/`](website/) | Website and docs: Astro + Starlight | [Local development](https://outbriefapp.com/en/docs/development/) |

Each folder is its own pnpm project: run `pnpm install` and `pnpm test` inside it. See the [development docs](https://outbriefapp.com/en/docs/development/) and the [ADRs](https://outbriefapp.com/docs/adr/).

## License

[OutBrief License](LICENSE) (Apache License 2.0 plus extra terms, following the [Multica License](https://github.com/multica-ai/multica/blob/main/LICENSE)).
