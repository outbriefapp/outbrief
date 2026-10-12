---
title: "Quick start"
description: "Deploy the server, install the daemon and the app, and pair devices into one account."
---

Calls stay inside one anonymous account. Let the daemon on the computer that runs the agents create the account. The desktop app and the phone app join that account. When a server is already running (for example `https://api.outbriefapp.com`), start at step 2 and put that URL in the daemon and the app. If signup is closed, you cannot create an account. Join with a pairing code from the owner.

1. **Deploy [outbrief-server](/en/docs/server/).** Run exactly one process. On a private deploy (the default), every start prints a one-time claim code while the server has no owner yet: `Claim code: XXXX-XXXX-XXXX`. Write down the server URL. Phones and other computers must be able to open that URL. `http://127.0.0.1:8787` works only on the machine that runs the server. When a phone will pair, put a LAN IP or a public `https://` URL in the daemon and the app. Commands are under [Deploy](/en/docs/server/#deploy).
2. **Install [outbrief-daemon](/en/docs/daemon/)** on the computer that runs the agents. After `pnpm install`, run `node src/cli.ts login --server <server-url>` and enter the claim code. The terminal prints a QR code and a 6-digit code. On macOS, then run `node src/cli.ts install` (start at login, and write the Claude Code / Codex Stop hooks). `install` stores the absolute path of node and `src/cli.ts` in launchd, so leave the checkout where it is.
3. **Install the desktop app ([outbrief-app](/en/docs/app/))** on that same computer. `pnpm tauri build` writes installers to `src-tauri/target/release/bundle/`. For development, `pnpm tauri dev`. When the daemon is already running, the desktop app joins that daemon's account the first time it opens.
4. **Install the phone app ([outbrief-app](/en/docs/app/), the same repo as the desktop app).** In that repo, `scripts/build-android.sh` writes the installable Android package to `dist-android/OutBrief.apk` and includes call pushes. For development, `pnpm tauri android dev` and `pnpm tauri android build` include pushes only after `export OUTBRIEF_FIREBASE_CONFIG=<google-services.json>`. An APK aimed at `https://api.outbriefapp.com` has to be built with that server's Firebase project. A project you create yourself cannot be woken by it, and that `google-services.json` is not in the repo. For iOS: `pnpm tauri ios init`, then `pnpm tauri ios dev` or `pnpm tauri ios build`. This version has no call pushes on iOS. You need the [Tauri mobile prerequisites](https://tauri.app/start/prerequisites/). That repo does not ship a store binary. After install, follow ["Calls while the phone app is in the background"](/en/docs/app/#calls-while-the-phone-app-is-in-the-background) in the app docs: turn on background calls, notifications, the lock-screen call screen, and pop-up calls, set battery to unrestricted, and check that Push wake-up says On. To pair, open Settings → Devices → Add a device on a device already in the account, or run `node src/cli.ts pair` on the computer that runs the daemon, and scan the QR code with the phone camera.

### Pairing

There is no login and no shared password. The first device creates the account. Later devices join with a 6-digit code. The code lasts 10 minutes and works once. The QR code and the link look like `outbrief://pair?server=<server-url>&code=<6 digits>&key=obk1_…`. The server URL and the end-to-end key go from device to device. The server never sees the key.

| Already in the account | Device joining | What to do |
|---|---|---|
| Daemon running on this computer | Desktop app on the same computer | Automatic. The app reads `~/.outbrief/local-api.key` and asks `127.0.0.1:8790` for a pairing code and the key |
| Desktop app, or the daemon (`node src/cli.ts pair`) | Phone app | Scan the QR code from Settings → Devices → Add a device, or the QR code in the terminal |
| Phone, or an app on another computer | Daemon on a computer | Copy the command from Add a device and run `node src/cli.ts login 'outbrief://pair?…'` on that computer |
| Any paired device | Desktop app on another computer | Paste the pairing link into the welcome screen. The desktop app does not open the camera |
| 6 digits only | Daemon or app | Also enter the same sentence from Settings → Encryption (at least 12 characters). When that sentence was never set, use the QR code or the link that carries `key=` |

The app can create the account instead. On the welcome screen, enter the server URL and the claim code, then choose Create a new account. Copy the link from Add a device and `login` the computer with it. After the daemon on that computer is paired and running, the desktop app on that computer joins the daemon's account.

Phone only, no desktop app: finish steps 1 and 2, then scan the QR code the daemon printed at login. Multica, the LLM, and the report language on the phone are encrypted and relayed through the server to that computer's daemon. The computer has to be online.

On a public server, set `OUTBRIEF_OPEN_SIGNUP=true`. The first device creates an account with no claim code.
