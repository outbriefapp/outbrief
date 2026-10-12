---
title: "Self-host the server"
description: "outbrief-server: a relay-only server. Deploy, environment, and local development."
sidebar:
  label: "Server"
---

OutBrief server. It only relays. [outbrief-daemon](/en/docs/daemon/) sends an encrypted report (including the spoken brief). The server holds that ciphertext in MySQL and pushes a call to clients over SSE. After hang-up it queues the client's encrypted reply back to the daemon. The server has no key, so it cannot read the report, the brief, or the reply. There is no login. An account is anonymous. Each device has its own token. Later devices join that account with a pairing code.

The full API reference and protocol notes are on the [Chinese version of this page](/docs/server/). The design decisions are in the [ADRs](/en/docs/adr/) (Chinese). This page covers deploying and running the server.

| Repo | Role |
|---|---|
| `outbrief-server` (this page) | Hono server + MySQL |
| [`outbrief-app`](/en/docs/app/) | Tauri 2 + React 19 client (desktop and mobile) |
| [`outbrief-daemon`](/en/docs/daemon/) | One resident process per agent computer. Claude Code / Codex stop hooks live here |

For the install order and device pairing, see [Quick start](/en/docs/quick-start/).

## Deploy

Run **one instance**. Presence, SSE, settings relay, and rate limits live in process memory ([ADR 0009](/en/docs/adr/0009-single-instance-first/)). Keep the replica count at 1. Publish by stopping the old process and then starting the new one (Kubernetes `strategy: Recreate`). A rolling update or a blue-green deploy runs two versions at once and drops calls. Back up MySQL.

### Run it directly

Node ≥ 22.18, pnpm 9, MySQL 8. Migrations in `db/migrations/` run when the process starts.

```bash
pnpm install
OUTBRIEF_DATABASE_URL='mysql://USER:PASSWORD@HOST:3306/outbrief' PORT=8787 pnpm start
```

The deployment environment injects `OUTBRIEF_DATABASE_URL`. The user and password above are placeholders. Use your own account in production and keep the connection string out of the repo. The local-development password lives only in [`db/bootstrap.sql`](https://github.com/outbriefapp/outbrief-server/blob/main/db/bootstrap.sql).

Until someone claims the server, the log prints `Claim code: XXXX-XXXX-XXXX`. The first device (the app welcome screen, or `outbrief-daemon login`) enters that code. After the claim, signup is closed and other devices join with a pairing code. A public server sets `OUTBRIEF_OPEN_SIGNUP=true`.

Health check: `GET /healthz`.

### Environment

Nothing is required. The process starts with no `.env`. [`.env.example`](https://github.com/outbriefapp/outbrief-server/blob/main/.env.example) only lists the defaults you can override.

| Variable | Default | Meaning |
|---|---|---|
| `OUTBRIEF_DATABASE_URL` | `mysql://outbrief:outbrief_local@127.0.0.1:3306/outbrief` | Injected by the platform. The default is the local docker database `outbrief` |
| `PORT` | `8787` | Listen port |
| `OUTBRIEF_OPEN_SIGNUP` | `false` | `true`: anyone may create an account (public server, rate limited). `false`: the first account needs the claim code from the log, then signup closes |
| `OUTBRIEF_FCM_SERVICE_ACCOUNT` | unset | Call pushes (ADR 0010). The key of a Firebase project service account: the whole JSON file (a platform variable), or the path to the file. Unset: no pushes, and a phone gets calls only while it holds its event stream. Set but unreadable: the server does not start. Keep the key out of the repository |
| `OUTBRIEF_TEST_DATABASE_URL` | `…/outbrief_test` | Test database. The name must end in `_test` |

### Railway

[`railway.json`](https://github.com/outbriefapp/outbrief-server/blob/main/railway.json) sets the health check and the restart policy. The rest is configured in Railway:

- Add a MySQL 8 service (image `mysql:8.4`, volume on `/var/lib/mysql`). Private network only. Leave the public TCP proxy off.
- On the server service, set `OUTBRIEF_DATABASE_URL` to the MySQL service's `MYSQL_URL` (`${{MySQL.MYSQL_URL}}`). Keep that connection string out of the repo and out of docs. Set `PORT=8787`. To push calls, paste the whole service account key file of the Firebase project into `OUTBRIEF_FCM_SERVICE_ACCOUNT`.
- One replica. Attach a volume to the server (for example `/data`; the code does not read or write it). Railway then stops the old deployment before starting the new one, and it will not scale past one replica.
- Add the CNAME and `_railway-verify` TXT records Railway shows. With Cloudflare, use DNS only (grey cloud) until Railway has issued the certificate.

### Call pushes

A phone off its event stream (app swiped away, phone asleep, between networks) is woken by a push ([ADR 0010](/en/docs/adr/0010-wake-phones-with-push/)). The push is queued in the transaction that stores the call, for every device of the account that registered an FCM token, and sent once that commits. It carries no content, only the call's id, `seq` and arrival time: the phone fetches the ciphertext with its own token and decrypts it. The phone then sends a receipt (`POST /v1/events/:id/receipt`). Without one the server pushes again every 10 seconds, at most 3 times, and stops once the call is taken or ended, or after the 2-minute ring window. The Android app has to be built with the same Firebase project. How to build it, and how to check Push wake-up on the phone, are in the app docs under ["Android package"](/en/docs/app/#android-package) and ["Calls while the phone app is in the background"](/en/docs/app/#calls-while-the-phone-app-is-in-the-background). The server keeps the FCM token of each phone, and the time and result of each push and receipt for a day.

### Local development

Node ≥ 22.18, pnpm 9, and Docker. The database is a local MySQL 8 container. Its name must be `some-mysql`, and it must publish container port 3306 on the host's 3306. The default URL is `mysql://outbrief:outbrief_local@127.0.0.1:3306/outbrief`. A different host port does not match that URL. The commands below, and `pnpm dev:all` in outbrief-app, look up the container by this name. They do not create it.

Create the container once. The image is `mysql:8.4`, the same image the Railway section uses. `MYSQL_ROOT_PASSWORD` is only the root password of this machine. Production does not use it.

```bash
docker run -d \
  --name some-mysql \
  --restart unless-stopped \
  -e MYSQL_ROOT_PASSWORD='local-root-password' \
  -p 3306:3306 \
  mysql:8.4
```

Wait until it answers. This check takes no password:

```bash
until docker exec some-mysql mysqladmin ping -h127.0.0.1 --silent; do sleep 1; done
```

Then:

```bash
pnpm install
# 1. Create the database and user once. At the password prompt, type the MYSQL_ROOT_PASSWORD from above
docker exec -i some-mysql mysql -uroot -p < db/bootstrap.sql
# 2. Create tables (startup does this too)
pnpm db:migrate
# 3. Start. The log prints a claim code while the server has no owner
pnpm dev
```

`db/bootstrap.sql` creates the databases `outbrief` and `outbrief_test`, and the user `outbrief` with password `outbrief_local`. That password is for this machine only. If the container already exists, do not run `docker run` again. If it is stopped, run `docker start some-mysql`. If something else is listening on 3306, stop that process before creating the container.

Schema changes are new files `db/migrations/NNN_*.sql`, applied once in filename order. Applied versions are recorded in `schema_migrations`. Add a new file. Leave a file that has already been applied alone.

## Commands

| Command | What it does |
|---|---|
| `pnpm lint` / `pnpm format` | Biome check / write |
| `pnpm typecheck` | `tsc` |
| `pnpm test` | Vitest against `OUTBRIEF_TEST_DATABASE_URL` (must be a `*_test` database; the tests truncate it) |
| `pnpm db:migrate` | Apply migrations that have not run |
| `pnpm dev` | Run the server and restart it when the code changes |
| `pnpm start` | Run the server |

## License

[OutBrief License](https://github.com/outbriefapp/outbrief-server/blob/main/LICENSE) (Apache License 2.0 plus extra terms, following the [Multica License](https://github.com/multica-ai/multica/blob/main/LICENSE)).
