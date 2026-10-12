---
title: Changelog
description: What changed in each OutBrief release.
---

Versions follow the [GitHub Releases](https://github.com/outbriefapp/outbrief-app/releases) of `outbrief-app`.

## 0.0.1 · 2026-10-12

The first public build.

- **Installers**: macOS Apple Silicon (`.dmg`, ad-hoc signed) and Android arm64 (`.apk`, with call pushes).
- **Calls**: when an agent finishes, every device of the account rings. Pick up for a spoken brief with cards. Hang up to reply to the same session or Multica comment.
- **Daemon**: takes Claude Code / Codex stop hooks and Multica workspaces, and writes briefs with the LLM configured on that computer.
- **Server**: a relay for ciphertext only, with anonymous accounts, device pairing, and push wake-up for Android calls ([ADR 0010](/docs/adr/0010-wake-phones-with-push/)).
