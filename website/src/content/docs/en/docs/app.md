---
title: "App"
description: "outbrief-app: the desktop and phone client. Calls, voice, and LLM settings."
---

OutBrief client (Tauri 2 + React 19 + TypeScript). When an AI agent finishes, the app rings like a video call. You answer, hear a spoken brief with cards, type to interrupt, and hang up with a reply back into the original session.

- Server: [`outbrief-server`](/en/docs/server/) (design notes and ADRs live there)
- Resident process and agent hooks: [`outbrief-daemon`](/en/docs/daemon/)

The [Chinese version of this page](/docs/app/) is the map of the source tree. This page is how to install the desktop app and the phone app, how they pair with the daemon, and how to use a call, the voice platforms, and the LLM page.

The interface follows Settings → Display language: the system language, 中文, or English. In Chinese the product name is 启奏. In English it stays OutBrief. Menu paths below are the English ones. The Chinese paths are 设置 → 设备 → 添加设备 and 设置 → 加密.

## Install order

Calls stay inside one anonymous account. Let the daemon on the computer that runs the agents create the account. This app, on the desktop and on the phone, joins that account.

1. **Deploy [outbrief-server](/en/docs/server/).** Run exactly one process. On a private deploy (the default), every start prints a one-time claim code while the server has no owner yet: `Claim code: XXXX-XXXX-XXXX`. Write down the server URL. Phones and other computers must be able to open that URL. `http://127.0.0.1:8787` works only on the machine that runs the server. When a phone will pair, put a LAN IP or a public `https://` URL in the daemon and in the app.
2. **Install [outbrief-daemon](/en/docs/daemon/)** on the computer that runs the agents. After `pnpm install`, run `node src/cli.ts login --server <server-url>` and enter the claim code. The terminal prints a QR code and a 6-digit code. On macOS, then run `node src/cli.ts install` (start at login, and write the Claude Code / Codex Stop hooks). `install` stores the absolute path of node and `src/cli.ts` in launchd, so leave that checkout where it is.
3. **Install the desktop app** from this repo on that same computer. Next section. When the daemon is already running, the first launch joins that daemon's account.
4. **Install the phone app** from this same repo. Next section. On a device that is already paired, open Settings → Devices → Add a device, or run `node src/cli.ts pair` on the computer, and scan the QR code with the phone camera.

For local development, create the `some-mysql` container first ([outbrief-server local development](/en/docs/server/#local-development)), then `pnpm dev:all` starts that container, the server in the sibling directory, and the desktop window. The script does not create the container. A production deploy still follows the order above.

### Install this repo

Node ≥ 22.18, pnpm 9, Rust stable ≥ 1.85, and the [Tauri prerequisites](https://tauri.app/start/prerequisites/).

Desktop:

```bash
pnpm install
pnpm tauri build
```

Installers land in `src-tauri/target/release/bundle/`. The build machine produces the installers for its own OS. Hot reload during development:

```bash
pnpm tauri dev
```

The phone app is this same project. The Android and iOS projects are not checked in. Generate them locally, then compile. Android needs Android Studio. iOS needs Xcode. See the mobile section of the Tauri prerequisites.

```bash
pnpm install
pnpm tauri android init
pnpm tauri icon app-icon.json   # init writes Tauri's default icons; this swaps in OutBrief's
pnpm tauri android dev      # device or emulator; export OUTBRIEF_FIREBASE_CONFIG first to include call pushes
scripts/build-android.sh    # installable APK with call pushes; see Android package
pnpm tauri ios init
pnpm tauri ios dev          # pnpm tauri ios build for a release. This version has no call pushes on iOS
```

This repo does not ship a store binary.

`pnpm dev` runs only the web UI in a browser (port 1520). A desktop window is `pnpm tauri dev`.

### Pairing

There is no login and no shared password. The first device creates the account. Later devices join with a 6-digit code. The code lasts 10 minutes and works once. The QR code and the link look like `outbrief://pair?server=<server-url>&code=<6 digits>&key=obk1_…`. The server URL and the end-to-end key go from device to device. The server never sees the key. The server URL inside the link has to be one the new device can open.

| Already in the account | Device joining | What to do |
|---|---|---|
| Daemon running on this computer | Desktop app on the same computer | Automatic. The app reads `~/.outbrief/local-api.key` and asks `127.0.0.1:8790` for a pairing code and the key. If the daemon is installed and stopped, the welcome screen waits, and the app joins once the daemon is running |
| Desktop app, or the daemon (`node src/cli.ts pair`) | Phone app | Scan the QR code from Settings → Devices → Add a device, or the QR code in the terminal |
| Phone, or an app on another computer | Daemon on a computer | Copy the command from Add a device and run `node src/cli.ts login 'outbrief://pair?…'` on that computer |
| Any paired device | Desktop app on another computer | Paste the pairing link into the welcome screen. The desktop app does not open the camera |
| 6 digits only | Daemon or app | Also enter the same sentence from Settings → Encryption (at least 12 characters). When that sentence was never set, use the QR code or the link that carries `key=` |

The app can create the account instead. On the welcome screen, enter the server URL and the claim code, then choose Create a new account. Copy the link from Add a device and `login` the computer with it. After the daemon on that computer is paired and running, the desktop app on that computer joins the daemon's account.

Phone only, no desktop app: deploy the server, `login` the daemon, and scan the QR code in the terminal. Multica, the LLM, and the report language on the phone are encrypted and relayed through the server to that computer's daemon. The computer has to be online. When the account has several computers, pick one at the top of the settings page.

On a public server, set `OUTBRIEF_OPEN_SIGNUP=true`. The first device creates an account with no claim code.

### Calls while the phone app is in the background

With the app on screen, calls arrive on the event stream. Desktop and phone both ring. Nothing else to turn on.

On Android, calls still ring after the app is swiped away or the screen is off:

1. Open Settings → Background calls and turn on receiving calls while the app is in the background or on the lock screen.
2. On the same page, allow Notifications, Incoming call on the lock screen, and Pop up incoming calls. Pop up is the system permission to display over other apps. On Xiaomi, OPPO, and vivo, also allow popping up in the background.
3. Set battery use to Unrestricted. Some phones also need OutBrief allowed to auto-start and run in the background.
4. Read Push wake-up on that page:
   - **On**: the server can wake the phone with a Google push after the app process is gone. No second app to install.
   - **Off**: open the app once and look again. Still off means this server has no `OUTBRIEF_FCM_SERVICE_ACCOUNT`, or the APK's Firebase project is not the server's.
   - **Not in this build**: the APK was built without `google-services.json`. Calls arrive only while the app is open and holding its event stream.

The phone needs Google Play services. This version has no push wake-up on iOS, so a call arrives there only while the app is open. A call this phone hears of more than 2 minutes after the server received it does not ring. It is listed under missed calls, and you can answer it from there.

After pairing, the pages you fill in on each device:

- Settings → Voice — report language and the speech platform. Speech is requested from this device. Details below.
- Settings → LLM — the model used for questions during a call. Saving also writes it to the local daemon when that daemon is running, which is what generates the brief. Details below.
- Settings → Multica — your Multica API token. It is stored on the computer's daemon. From the phone it is encrypted and relayed. The server does not see it.

## During a call

- The idle page has two icons at the top right, Calls and Settings. The green round phone button under the status text is how you dispatch a new task.
- The top left is today's mode, the same idea as a phone focus mode. The capsule shows a bell and the mode name. While calls are silenced the bell is crossed out and the capsule turns amber. Open it to see whether calls ring now and when that changes, then pick Ring anytime or one of today's modes. Manage modes… opens Settings → Modes.
- A Multica call shows the issue's project name on the calls page and in the call header. An issue with no project shows no project name.
- When a report arrives, this device synthesizes the whole brief before the phone rings. Answering does not wait on speech. The idle page says it is generating speech for N reports. If synthesis fails, that report does not ring. The idle page shows the reason. Retry generates it again and rings when it succeeds. Ignore marks it declined.
- A report that arrives outside the ringing hours does not ring and does not open a window. It is a missed call. It does not ring by itself when the hours start again. The calls icon shows a red count. Open it and answer the one you want, or tap Acknowledge all. Entering quiet hours during a call does not cut the call off. Reports still queued at hang-up become missed calls.
- Interrupt playback by typing, or by tapping a decision. The field starts at three lines and grows with the text, up to a limit.
- The transcript has a fixed height and scrolls inside it, so Pause and Hang up stay on screen. A short window shrinks the cards first.
- A typed question is answered by the LLM from the full report and the brief, and the answer is read as it arrives. A Multica call works the same way. An instruction or opinion for the agent gets "Noted. I'll send it to the agent when you hang up." The app does not pretend it already ran.
- Typing continue, go on, or keep playing resumes the brief.
- Interrupting an answer cancels the request and the speech still queued. A failed answer shows the reason in the bubble (no LLM configured, HTTP 401, and so on) and a Retry button.
- Hang up builds a reply from what you said and the decisions you picked. Edit it, then send it to the agent. That step does not call the LLM.
- Flip cards with the dots or ‹ ›. Speech jumps to the start of that card.
- If the speech platform (this device calls it directly; see Settings → Voice) cannot synthesize before the ring, the idle page shows the failure. Retry or ignore it. If speech fails during the call, the call drops to cards and text, and the captions advance by length.
- When the brief finishes, the call stays open. Ask another question, or hang up and reply. Done answers the next call.

## Settings → Voice

Speech for the brief is requested from this device, from the platform you pick. The server never sees the brief in the clear. Top to bottom: **report language**, **built-in platform / custom endpoint**, the fields that platform needs, **model**, **voice**, **rate**, **test synthesis**.

- **Layout.** Same shape as Settings → LLM. Two tabs, Built-in and Custom, one at a time. Built-in platforms are one searchable dropdown. Each row is the name plus the host, Free, or Self-hosted. Each platform declares its own fields (key, region, base URL). Adding a platform does not require a UI change.
- **One saved config per platform.** Switching platforms does not wipe the previous key, model, or voice. The API key stays on this device. After save it shows `sk-…abcd` and a Replace button.
- **Voices** are grouped female / male and shown by name, not by the raw id. Platforms with cloned voices (MiniMax, Doubao, ElevenLabs, and others) show the real id only after you focus the field. Platforms that can list voices have a Fetch voices button.
- **Voice and language:**
  - Per language: Azure (a voice list per locale) and MiniMax (the voice id carries the language; Cantonese is its own group). Changing the report language switches to a voice for that language.
  - Independent of language: ElevenLabs, Grok, OpenAI, Gemini, and Alibaba Qwen-TTS. The voice list does not change with the report language. The language is a request parameter.
  - Doubao: ids are prefixed `zh_` / `en_`, but 2.0 (`_uranus_`) voices cover 30+ languages, so the picker is one list.
  - Tencent Cloud: voices are split by language, and `PrimaryLanguage` is only Chinese or English, so other report languages do not fit this platform.
- **Rate** is a 0.5×–2× slider, then clamped to that platform's own range (ElevenLabs 0.7–1.2, Grok 0.7–1.5, Tencent Cloud mapped onto its `Speed` steps). Gemini, Alibaba Qwen-TTS, ChatTTS, and CosyVoice have no rate parameter. The slider says so.
- **Test synthesis** speaks one line with the values currently on the page. You do not have to save first. A failure shows the platform's own error.

### Built-in platforms

Checked against each vendor's docs on 2026-09-29.

| Platform | Endpoint | Notes |
|---|---|---|
| Azure | Free translator JWT (path B) | Default. Nothing to configure. Voices for 90 locales follow the report language |
| ElevenLabs | `POST /v1/text-to-speech/{voice_id}` | `output_format` is a query parameter. Any voice can read every language that model supports, but the catalog is per account. The built-in list only has the two ids in the official docs. Fetch the rest. Eleven v4 has no rate |
| MiniMax | `POST /v1/t2a_v2` | China and international accounts and keys are separate. Errors are HTTP 200 plus `base_resp.status_code`. Audio is hex. System voices are per language |
| Doubao | `POST /api/v3/tts/unidirectional/sse` | New console: API key. Old console: App ID plus access token. The voice picks the resource id (a 2.0 voice with a 1.0 resource returns 45000000). Audio is base64, one frame at a time |
| Alibaba Bailian | Qwen3-TTS `multimodal-generation/generation` | Beijing and Singapore keys are not interchangeable. The response is an audio URL valid for 24 hours; the app downloads it. No rate parameter |
| Tencent Cloud | `TextToVoice` (API 3.0) | TC3-HMAC-SHA256 via WebCrypto. The signature date is UTC. Errors are HTTP 200 plus `Response.Error`. Premium voices are 16 kHz |
| OpenAI | `POST /v1/audio/speech` | The base URL can point at a proxy or a compatible server |
| Gemini | `generateContent` with `responseModalities: ["AUDIO"]` | Asks for `AUDIO_WAV`, because 3.1 / 2.5 otherwise return raw L16 PCM. No rate parameter. Fast or slow goes in `speech_metadata.style` |
| Grok | `POST /v1/tts` | `language` is required. A report language outside its 20 languages is sent as `auto` |
| ChatTTS | Self-hosted `/v1/audio/speech` from `examples/api/openai_api.py` | The other official route, `/generate_voice`, returns a zip, so the app uses this one. Its source hard-codes `[speed_5]`, so rate has no effect |
| IndexTTS | Self-hosted vLLM-Omni `/v1/audio/speech` | The Gradio WebUI in that repo takes positional arguments that change between versions, so it is not the API. Use Custom for Gradio. The voice is a reference audio file you upload |
| CosyVoice | Self-hosted FastAPI `/inference_sft` | The body is raw PCM, and `Content-Type` is still `text/plain`. The sample rate must match the model (CosyVoice-300M 22050, CosyVoice2/3 24000). The app adds the WAV header. The server does not forward rate |

### Custom endpoint

TTS APIs do not share one body shape, so the app does not guess. The only input is a pasted cURL example:

- Paste the vendor's curl sample and press Import. Method, URL, headers, and body are filled in one step.
- Import maps common field names (`text` / `input` / `tts_text`, `voice` / `speaker` / `spk_id`, `speed` / `speech_rate`) onto `{{text}}`, `{{voice}}`, and `{{speed}}`, including nested JSON, form fields, and query parameters. A numeric rate stays a number.
- If a field was recognized wrong, the three rows "which value is the sentence / voice / rate" list the real sample values. The selected one is checked. Tap another. You do not type braces.
- The request that will be sent is shown read-only. To change it, paste the curl again.
- Response is either "the body is the audio" or "a field inside JSON". For JSON, give the path (`data.audio`, `candidates.0.content.parts.0.inlineData.data`) and whether the field is base64, hex, or a URL. A URL is downloaded. A stream of one JSON object per line is concatenated in order.
- mp3, wav, and ogg are detected from the header. Raw PCM is "raw PCM (16-bit mono)" plus a sample rate. The app adds the WAV header.
- Substitution escapes the sentence for the body's Content-Type (JSON string, form, XML/SSML). Quotes and newlines in the sentence do not break the body. A missing sentence, a non-http(s) URL, or an empty field path is rejected before anything is sent.

## Settings → LLM

Questions during a call (this device calls the model) and briefs on the computer (the local outbrief-daemon calls the model) use the same model. This is the only page that configures it.

- **Layout.** Two tabs, Built-in provider and Custom endpoint. Built-in providers are one searchable dropdown. Each row is the name and the API host.
- **Providers.** OpenAI, Claude, Grok, DeepSeek, Gemini, OpenRouter, Qwen, Doubao, Kimi, Zhipu GLM, SiliconFlow, Vercel (AI Gateway), and Ollama (`localhost:11434`) fill the base URL from `src/llm/providers.ts`. Addresses, model ids, and whether `json_schema` works were checked against each vendor's docs on 2026-09-29. Anything else that speaks the OpenAI API, including a server you run, goes under Custom. A bare host gets `/v1`. A pasted `…/chat/completions` URL is cut back to the base URL.
- **API key.** The eye icon shows it. After save the page says "Saved sk-…abcd" and offers Replace. The key itself is hidden. Changing provider requires a new key. Ollama does not check the key. Type anything.
- **Model.** The dropdown lists that provider's built-in recommendations first (the first one is selected when you pick the provider), then the models `GET {base URL}/models` returns for this key. Search, or type any model id.
- **Brief output.** The brief asks the model for JSON. Two strategies, not tied to a model. A built-in provider only sets a default:
  - **JSON Schema.** The request sends `response_format: json_schema` and the API enforces it. OpenAI, Gemini, Grok, Qwen, Doubao, Kimi, Ollama, and others.
  - **JSON mode.** For models without `json_schema` (DeepSeek, Zhipu, Claude's OpenAI-compatible API, and local models that lack it). The schema goes in the prompt, the request sends `response_format: json_object`, a markdown fence is stripped, and the same schema validates the result.
- **Test connection** makes one call in the selected output mode, with a small schema shaped like a brief, the same kind of call the daemon makes. Three outcomes: it works; the model chats but this output mode does not (under JSON Schema a button switches to JSON mode and tests again); it fails and says why (bad key, unknown host, unknown model).
- **Save** writes this device's local settings. When the server is connected and the local daemon is running, it also `PUT`s `http://127.0.0.1:8790/llm/settings`, and the next brief uses it. No restart. From a phone, or from an app aimed at another computer's daemon, save stays on this device unless you check "Also use this for briefs on «computer»". That copy is encrypted and relayed. The two machines may not reach the same URL (a `127.0.0.1` address exists only on that computer). There is no backup channel. The key moves between this device and that computer's daemon. The server does not see it.
- **Clear** removes the config on this device and on the daemon.

Every call goes through the AI SDK's `@ai-sdk/openai-compatible`. There is no per-vendor adapter.

With nothing configured, a question fails and tells you to fill this page in. On a first launch, `VITE_OUTBRIEF_LLM_BASE_URL`, `VITE_OUTBRIEF_LLM_API_KEY`, and `VITE_OUTBRIEF_LLM_MODEL` prefill the page when they were set at build time. `pnpm dev:all` reads `llm.primary` from `${OUTBRIEF_HOME:-~/.outbrief}/daemon.json` and passes those three in. It does not print the key. Once you save the page, the saved values win.

## Desktop

Closing the window only hides it in the tray. A call brings the window forward. It does not start at login.

Only one window (`tauri-plugin-single-instance`). Two windows would each ring the same call. Opening the app again brings the window already running to the front. `pnpm dev:all` closes a desktop app that is already running, including one from another worktree, before it starts.

## Local development

Node ≥ 22.18, pnpm 9, Rust stable ≥ 1.85 (Tauri needs edition 2024), and the [Tauri prerequisites](https://tauri.app/start/prerequisites/).

This repo does not create the database. First follow [outbrief-server local development](/en/docs/server/#local-development): a MySQL 8 container named `some-mysql` publishing host port 3306, and `db/bootstrap.sql` applied once. `pnpm dev:all` only starts a container that already exists. If the name is missing, the script exits.

One command then starts the server and the desktop window. Quit from the tray, or Ctrl+C, stops both:

```bash
pnpm dev:all        # scripts/dev.sh; server log at logs/server.log
```

The server directory defaults to the sibling whose name is this directory with `app` replaced by `server` (`outbrief-app` → `outbrief-server`, worktree `app-p1` → `server-p1`). Another location is `OUTBRIEF_SERVER_DIR=<dir>`. The server does not need a `.env`. The script passes the server URL to the desktop app. While the server has no owner, it also passes the claim code from the log, and the desktop app creates the first account. If an outbrief-daemon is already running on this machine, the app joins that daemon's account instead. When `daemon.json` has `llm.primary`, those values prefill Settings → LLM. The key is not printed. The desktop app has no login.

Desktop window only, or the UI only:

```bash
pnpm install
pnpm tauri dev      # desktop window, hot reload (the UI dev server is on :1520, not the :1420 other Tauri apps use)
pnpm dev            # UI only, in a browser
```

A desktop window started on its own joins or creates an account as in Accounts and devices above. When that cannot finish, the welcome screen asks for the server URL (default `http://localhost:8787`) and a claim code or a pairing code.

## Android package

`scripts/build-android.sh` builds an arm64 release APK you install on a phone. It is not a store build. Install rustup first (`rustup target add aarch64-linux-android`; Homebrew's rust has no Android target), JDK 17, the Android SDK (`platforms;android-36`, `build-tools`), and the NDK. Point `ANDROID_HOME`, `NDK_HOME`, and `JAVA_HOME` at them.

- The server URL defaults to `https://api.outbriefapp.com`. Override it with `VITE_OUTBRIEF_SERVER_URL`. A phone cannot open the computer's `localhost`. That default server already has an owner, so a new account cannot be created on it. Point a server you deploy at your own URL. Join an existing account with a pairing code. After install, open Settings → Devices → Add a device on the computer and scan the code.
- `src-tauri/gen/` is not in the repo. The script runs `tauri android init` when that directory is missing, regenerates icons from `app-icon.json`, then adds the camera and microphone permissions and the release signature. The first build creates the keystore under `~/.outbrief-android/` with a random password, outside the repo. Later builds keep that key, so an update can replace the installed app.
- Call pushes (ADR 0010 of outbrief-server) are included by `scripts/build-android.sh`. The script reads `~/.outbrief-android/firebase/google-services.json`, or the file named by `OUTBRIEF_FIREBASE_CONFIG`, and passes it to the build. `pnpm tauri android dev` and `pnpm tauri android build` do not read that default path. Export `OUTBRIEF_FIREBASE_CONFIG` yourself, or the APK has no pushes.
- In the Firebase console, add an Android app with package name `ai.outbrief.app` and put the downloaded `google-services.json` at that path. Give the server the service account key of the same Firebase project, the whole JSON, in `OUTBRIEF_FCM_SERVICE_ACCOUNT`. A token registered from a different Firebase project never receives that server's pushes. The file stays out of the repo. Without it the APK still installs, and calls arrive only while the app is open and holding its event stream. An APK aimed at `https://api.outbriefapp.com` has to be built with that server's Firebase project. A project you create yourself cannot be woken by the official server. How to check on the phone is under [Calls while the phone app is in the background](#calls-while-the-phone-app-is-in-the-background).
- Android's system WebView implements `BarcodeDetector` with Google Play services. Declaring it without Play services crashes on scan, and many phones have no Play services. The Android build decodes QR codes with `qr-scanner`'s own worker (`src/components/QrScanner.tsx`).

## Commands

| Command | What it does |
|---|---|
| `pnpm dev:all` | Start an existing `some-mysql`, the sibling outbrief-server, and the desktop window. Does not create the container. See [Local development](#local-development) |
| `pnpm tauri dev` | Desktop window with hot reload (the UI dev server is on port 1520) |
| `pnpm dev` | UI only, in a browser |
| `pnpm tauri build` | Desktop installers |
| `pnpm tauri android init` / `ios init` | Generate the local mobile project. Then `dev` or `build`. See [Install this repo](#install-this-repo) |
| `scripts/build-android.sh` | Build a signed arm64 APK at `dist-android/OutBrief.apk` and include call pushes. See [Android package](#android-package) |
| `pnpm lint` / `pnpm format` | Biome check / write |
| `pnpm typecheck` | `tsc` |
| `pnpm test` | Vitest |

## License

[OutBrief License](https://github.com/outbriefapp/outbrief-app/blob/main/LICENSE) (Apache License 2.0 plus extra terms, following the [Multica License](https://github.com/multica-ai/multica/blob/main/LICENSE)).
