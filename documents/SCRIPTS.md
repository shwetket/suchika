# Scripts Reference

| | |
|---|---|
| **Type** | Reference |
| **Audience** | Developers |
| **Status** | Active |
| **Last updated** | 2026-07-13 |

## Objective

Document every script in `scripts/` — what it does, how to call it, and its short alias. The primary interface for day-to-day development is `dev-aliases.ps1`; this document explains what each alias does under the hood.

## Use Cases

- Looking up the exact flags for a script (`db-reset -Force`, `build-local.ps1 -SkipSonar`)
- Finding the alias for a common task without reading the script source
- Adding a new script — follow the conventions in the "Adding a New Script" section at the bottom

---

All developer scripts live in `scripts/`. The primary interface is `dev-aliases.ps1` — dot-source it once per terminal session.

```powershell
. .\scripts\dev-aliases.ps1   # loads all aliases; type help-dev to see them
```

---

## Quick Reference

| Want to... | Command |
|---|---|
| Start all services (visible windows, hot-reload) | `. .\scripts\dev-aliases.ps1` then `da` |
| Just get the app running, no windows | `run-local` (`rl`) |
| Start one service | `dp` / `dw` / `dh` / `dho` / `dg` / `dwb` |
| Run tests for a domain | `tp` / `tw` / `tsa` |
| Full pre-commit check | `bv` |
| Check if services are up | `status` |
| Stop everything started by `da` | `sa` |
| Stop everything started by `run-local` | `stop-local` (`stopl`) |
| Watch logs live | `lnav-dev` |
| Tail build/test log | `logs [service]` |
| Open database shell | `db-shell` |
| Reset database | `db-reset` (destructive) |
| Run SonarQube | `ss` |
| Regenerate API client | `gapi` |
| First-time setup | `setup-dev` |

---

## Single Source of Truth: `scripts/services.json`

Ports, DB schema names, Gradle module/task wiring, the DB password fallback, and Java/Node
version floors used to be hardcoded independently in `dev-aliases.ps1`, `dev-aliases.sh`,
`stop-all.ps1`, `health-check.ps1`/`.sh`, `dev-service.ps1`, and `check-prerequisites.ps1`/`.sh`
(six-plus files, each with its own copy of `8081`, `local_dev_only`, `21`/`25`, etc.). As of
2026-07-13 these all live in one file, `scripts/services.json`, and every script above reads
from it instead of repeating literals:

```json
{
  "services": [ { "name": "profile", "port": 8081, "schema": "profile", "gradleModule": "...", "devTask": "...", "healthPath": "/q/health", "startOrder": 1, "kind": "backend" }, ... ],
  "database": { "port": 5432, "name": "app_db", "appUser": "app_user", "adminUser": "postgres", "passwordFallback": "local_dev_only", "psqlWindowsPath": "C:\\Program Files\\PostgreSQL\\18\\bin\\psql.exe" },
  "versionFloors": { "javaHardFailBelow": 21, "javaTarget": 25, "nodeHardFailBelow": 20, "nodeTarget": 24 },
  "pidDir": "~/.suchika/run",
  "logDir": "~/.suchika/logs"
}
```

To change a port, schema, Gradle task, the DB password fallback, or a version floor: **edit
`services.json`**, not the scripts that consume it.

Two loader scripts expose this to each shell:

- **`scripts/config.ps1`** — dot-sourced; does `Get-Content services.json | ConvertFrom-Json` and
  exposes `$SuchikaServices`, `$SuchikaDb`, `$SuchikaVersionFloors`, `$SuchikaPidDir`,
  `$SuchikaLogDir`, plus helper functions `Get-SuchikaService -Name <svc>`,
  `Get-SuchikaServiceNames [-BackendOnly]`, `Get-SuchikaHealthUrl -Name <svc>`.
- **`scripts/config.sh`** — sourced; parses the same file with `grep -oP` (see below for why not
  `jq`) and exposes `suchika_svc_field <svc> <field>`, `suchika_db_field <field>`,
  `suchika_version_floor <field>`, `suchika_service_names`, plus `$SUCHIKA_DB_PASSWORD_FALLBACK`,
  `$SUCHIKA_PID_DIR`, `$SUCHIKA_LOG_DIR`.

**Why `grep -oP` instead of `jq` on the bash side:** `jq` is not guaranteed present (it isn't
installed by `.devcontainer/setup.sh`, and isn't on this repo's own dev machine either — checked
directly before choosing an approach) and pulling it in as a new dependency wasn't justified just
to read six small, flat JSON objects. Each `services.json` object is deliberately kept on a
**single line** specifically so `grep -oP '"field":\s*"?\K[^",}]*'`-style extraction is reliable
without a real JSON parser. `grep -oP` (PCRE) itself is not a new dependency — it ships with GNU
grep on both Codespaces (Debian) and Git Bash (MSYS), and was already relied on by
`health-check.sh` before this change. If `services.json` ever needs deeper nesting, revisit this
choice and switch to `jq` (adding it to `.devcontainer/setup.sh` and this repo's own prerequisite
list) rather than growing the hand-rolled parser.

`ci.yml`'s Postgres service container (port `5432`, password `local_dev_only`) is **not** wired to
`services.json` — GitHub Actions evaluates the `services:` block before checkout, so it can't read
a file out of the repo at that point. Those two values are kept in sync by hand; see the inline
comment in `ci.yml`.

---

## PID-File Service Registry

**`scripts/service-registry.ps1`** / **`scripts/service-registry.sh`** — the shared primitive
`dev-service.ps1` / `dev-aliases.sh`'s `_dev_svc`, `stop-all.ps1` / `stop-all()`, and
`health-check.ps1` / `.sh` all build on, added 2026-07-13. Not called directly by users.

On start, once a service's port actually starts LISTENing, the real owning OS process (the
`java.exe`/`java` running `quarkusDev`, or `node`/`node.exe` running `npm start` — **not** the
wrapper process that launched it) is resolved and written to
`<pidDir>/<service>.pid` as a small JSON record: `{"pid": ..., "processName": ..., "port": ..., "service": ..., "startedAt": ...}`.
`pidDir` defaults to `~/.suchika/run` (see `services.json`).

- **Windows** (`dev-service.ps1`) opens the service in a new GUI terminal window
  (`wt.exe`/`powershell.exe`), so the PID `Start-Process` returns is the terminal's PID, not the
  server's. `Register-SuchikaServiceAsync` instead starts a background `Start-Job` that polls
  `Get-NetTCPConnection` for the port and resolves the real owning process once it binds — this
  returns immediately so `dev-all`'s startup loop isn't blocked waiting on each service in turn.
- **Bash** (`dev-aliases.sh`'s `_dev_svc`) already backgrounds `gradlew` directly, but `$!` is
  still the `gradlew` wrapper PID, not the `java` process Gradle eventually forks.
  `suchika_register_service_async` does the same polling trick in a backgrounded subshell (via
  `lsof -ti tcp:$port`), for the same reason — both platforms end up tracking the same thing (the
  actual listening process), just resolved differently.

**Consumers:**
- `stop-all.ps1` / `stop-all()` (bash) check the PID registry first for each service — if a valid,
  live PID is recorded (verified alive **and** the process name still matches, guarding against
  PID reuse by an unrelated process), that exact process is killed and the pid file removed.
  **Falls back to today's port-based "kill whatever is LISTENing on this port" only when no valid
  PID record exists** — this keeps backward compatibility with anything already running the old
  way (e.g. a service started before this change, or manually via `quarkusDev` outside the
  scripts).
- `health-check.ps1` / `.sh` show the registered PID next to each UP/DOWN line as extra
  information (`[PID 12345 java]`) — informational only, does not affect the UP/DOWN verdict,
  which is decided purely by `/q/health` (see below).
- Stale pid files (process no longer alive, or a different process now holds that PID) are deleted
  automatically the next time anything reads them — there's no separate cleanup step to remember.

This was built as the shared primitive later phases would reuse rather than re-implement — see
"Simplified Local Run" below, which is the first of those to actually build on it (`run-local.ps1`
calls `Register-SuchikaServiceAsync` directly instead of writing its own PID-capture logic).

---

## Real Health Checks (`/q/health`)

As of 2026-07-13, all 5 backend `build.gradle.kts` files (`profile`, `wealth`, `health`,
`household` adapters, and `web-gateway`) declare `io.quarkus:quarkus-smallrye-health`, which
exposes `GET /q/health` (Quarkus's SmallRye Health extension — no custom endpoint code was
written; the dependency alone activates it with Quarkus's built-in liveness/readiness checks).

`health-check.ps1` / `.sh` (`status`) now hit `/q/health` instead of `/q/openapi`, and only count a
service as UP when the response is **HTTP 200 with body `"status": "UP"`** — previously *any*
response, including a raw HTTP 500, was treated as UP (because `/q/openapi` doesn't reflect actual
service health, and the old check only distinguished "got a response" from "connection refused").
A service that's listening but broken now correctly reports DOWN.

---

## Simplified Local Run: `run-local` / `stop-local`

Added 2026-07-13 (Phase 3). Two ways to bring the whole stack up now exist side by side, and
they're deliberately **not** the same thing:

| | `da` (`dev-all`) | `run-local` (`rl`) |
|---|---|---|
| Purpose | Active development | "I just want it running" |
| Windows | Opens a **visible terminal window per service** | **No windows at all** |
| Bash/Codespaces | Backgrounds each service (no window concept either way) | Same as `da` — see below |
| Hot reload console output | Visible live in each window | Only in the log file |
| Stop with | `sa` (`stop-all`) | `stop-local` (`stopl`) — same underlying kill logic as `sa` |

**Windows (`scripts/run-local.ps1`)** genuinely needed new logic here: `dev-service.ps1` always
opens a GUI window (`wt.exe new-tab` or a fallback `powershell.exe -NoExit` window), which is fine
for watching Quarkus's hot-reload console live but wrong for "just start the app so I can click
through the UI." `run-local.ps1`:
1. For each service, writes a tiny wrapper `.cmd` (in `%TEMP%\suchika-run-local\`) that `cd`s into
   the right directory. For the frontend, `npm start`'s stdout+stderr is appended straight into
   `~/.suchika/logs/web.log` (no conflict — npm has no file-logger of its own). For the four
   backend services, gradle's own stdout+stderr is appended into a **separate**
   `~/.suchika/logs/<service>.console.log` sidecar, **not** `<service>.log` — that path is already
   owned by Quarkus's own `%dev.quarkus.log.file.path` file appender, and on Windows having
   cmd.exe's `>>` handle and Quarkus's `SizeRotatingFileHandler` both target the same file caused a
   real, reproduced `LogManager error ... process cannot access the file` failure during testing
   (Quarkus happened to recover via its own retry that time, but it isn't reliable). `lnav-dev`
   still tails `<service>.log` as the canonical structured log; `<service>.console.log` exists only
   to diagnose a service that fails before Quarkus's own logger ever attaches (bad Gradle daemon,
   compile error, etc). Note: bash's `_dev_svc` (used by `da`) has the same
   redirect-into-Quarkus's-own-log-file pattern, but it's harmless there — POSIX allows renaming a
   file out from under an open handle, so it just duplicates lines rather than failing; not changed
   here since it predates this phase and stays in scope for a future cleanup rather than this one.
2. Launches that wrapper with `Start-Process -WindowStyle Hidden -PassThru` — no window, on any
   Windows session (console or RDP).
3. Calls `Register-SuchikaServiceAsync` (Phase 1's PID registry, **reused as-is, not
   reimplemented**) to resolve and persist the real `java`/`node` process once the port binds —
   the same reasoning as `dev-service.ps1`'s GUI-window case: the process `Start-Process` returns
   is a wrapper, not the actual server.
4. Starts services in the mandatory dependency order (profile → wealth/health/household → gateway
   → frontend), polling each one's real `/q/health` via the new shared `Wait-SuchikaHealthy`
   helper (`config.ps1`) before moving on — aborts early if profile itself never becomes healthy,
   since nothing else can start without it.

**Bash (`scripts/run-local.sh`) is a thin wrapper around the existing `dev-all`**, not a
reinvention — bash's dev mode was already headless (there's no "GUI window" concept in a
Codespaces/Linux terminal to bring to parity with), so `dev-all` already does everything
`run-local` needs. The separate `run-local.sh` file and `rl` alias exist purely so the command
reads the same on both platforms; the actual work happens in `dev-all`.

**`stop-local.ps1` / `.sh` are thin wrappers around `stop-all.ps1` / `stop-all()`** for the same
reason in reverse: `stop-all` already checks the PID registry first and falls back to port-based
kill, for *every* registered service regardless of whether it was started via `dev-service.ps1`
(GUI window) or `run-local.ps1` (headless) — both register into the identical
`~/.suchika/run/<service>.pid` files, so there is no headless-specific stop logic left to write.
Kept as their own named script/alias (rather than just telling users to run `sa`) because
`run-local`/`stop-local` reads as a matched pair, and it leaves a clean seam if headless-mode stop
semantics ever need to diverge later.

```powershell
.\scripts\run-local.ps1                # or: rl   (after dot-sourcing dev-aliases.ps1)
.\scripts\run-local.ps1 -TimeoutSec 300  # wait longer per service before giving up
.\scripts\stop-local.ps1               # or: stopl
```
```bash
bash scripts/run-local.sh              # or: rl   (after sourcing dev-aliases.sh)
bash scripts/stop-local.sh             # or: stopl
```

**When to use which:** `da`/`sa` while actively developing (you want to see Quarkus recompile and
reload live, and to spot a stack trace the instant it happens). `run-local`/`stop-local` when you
just need the app up — demoing, clicking through the UI, or as a prerequisite for something else —
and don't want six terminal windows open.

---

## Script Catalogue

### `services.json`, `config.ps1` / `.sh`, `service-registry.ps1` / `.sh`, `check-ps1-bom.sh`

See the sections above and "Adding a New Script" below. These are infrastructure consumed by
the other scripts, not called directly day-to-day.

### `run-local.ps1` / `.sh`, `stop-local.ps1` / `.sh`

See "Simplified Local Run" above.

---

### `dev-aliases.ps1` — Primary developer interface
Dot-source once per terminal session. Defines all short aliases and functions.

```powershell
. .\scripts\dev-aliases.ps1
help-dev    # print full alias list
```

**Build aliases:** `bp` `bw` `bh` `bho` `bg` `bwb` `ba` `bv` (plus unaliased `build-shared`)
**Dev aliases (visible windows):** `dp` `dw` `dh` `dho` `dg` `dwb` `da`
**Headless run (no windows):** `rl` (run-local) `stopl` (stop-local) — see "Simplified Local Run" above
**Test aliases:** `tp` `tw` `tsa` (plus unaliased `test-health`, `test-household`, `test-gateway`, `test-shared`, `test-web`)
**Quality:** `ss` (sonar-scan) `gapi` (generate-api)
**Control:** `sa` (stop-all) `status` `check`
**Database:** `db-start` `db-reset` `db-shell`
**Logs:** `logs` `lnav-dev`
**Maintenance:** `clean-builds` `clean-all` `setup-dev`

---

### `dev-aliases.sh` — Bash / Codespaces equivalent

Same alias surface as `dev-aliases.ps1`, sourced the same way, for Linux/bash shells (Codespaces auto-loads it into `~/.bashrc`; see `.devcontainer/setup.sh`).

```bash
. ./scripts/dev-aliases.sh
help-dev
```

Behavioral differences from the PowerShell version (all intentional, environment-driven — every
gap below is deliberate, not an oversight; confirmed during the 2026-07-13 parity audit):
- `dev-profile`/`dev-wealth`/etc. run each service as a **background** job writing to `~/.suchika/logs/<svc>.log`, instead of opening a new terminal window (there's no `dev-service.ps1`-style "new window" concept in a Codespaces terminal). Watch output with `lnav-dev` or `tail -f`. Both sides now also register the real server PID in the background (see "PID-File Service Registry" above) once the port binds.
- No `db-start` — Codespaces' `db` container is brought up and health-checked by `docker-compose`/`setup.sh` before the dev container is even usable, so there's no separate "start Postgres" step.
- No `sonar-start` — SonarQube is not run in Codespaces at all (too resource-intensive for the free tier; run `ss`/`sonar-scan` locally before opening a PR instead — see CLAUDE.md).
- **No `clean-all` (nuclear clean)** — `clean-all.ps1`'s job is to blow away everything git doesn't track (`node_modules`, `.gradle` cache, build outputs) and start over in place. In Codespaces the equivalent action is deleting and recreating the codespace itself (an ephemeral cloud container) rather than running a local destructive script against it — there's nothing for a bash `clean-all` to usefully do that "rebuild the codespace" doesn't already cover. `clean-builds` (the *safe*, non-destructive clean) is still available on both sides.
- **No `setup-dev`** — `setup-dev.ps1`'s job (prereqs check, `.env` from template, DB bootstrap, `npm install`) is handled automatically by `.devcontainer/setup.sh`, which runs once via the devcontainer's `postCreateCommand` when a codespace is first created (see `.devcontainer/devcontainer.json`) — there is no manual "first-time setup" step to invoke in Codespaces because it already ran before your first terminal opened. `check` (prerequisites) is still available standalone on both sides.
- `db-reset` / `db-shell -AsAdmin` use the same `PGPASSWORD` → `POSTGRES_PASSWORD` → `services.json`'s `passwordFallback` (`local_dev_only`) chain as `db-reset.ps1` (both now read the fallback from `scripts/services.json` via `config.sh`/`config.ps1` instead of hardcoding it separately). `local_dev_only` is correct out of the box in Codespaces (the `db` container is created with that password) and for `app_user` on any machine that's run `00_bootstrap.sql`, but on a natively-installed Windows Postgres the real superuser password is whatever was chosen at install time — export `PGPASSWORD` yourself first if so.

---

### `build-local.ps1` / `build-local.sh` — Full pre-commit verification

Builds all services in dependency order, runs ArchUnit, all tests, lint, Prettier, and optionally SonarQube. Mirrors CI exactly (see `documents/CICD.md`).

```powershell
.\scripts\build-local.ps1                  # full verification
.\scripts\build-local.ps1 -SkipSonar       # skip SonarQube (faster)
.\scripts\build-local.ps1 -SkipFrontend    # backend only
.\scripts\build-local.ps1 -FrontendOnly    # frontend only
```
```bash
bash scripts/build-local.sh                # full verification (Codespaces/Linux)
bash scripts/build-local.sh --skip-sonar
bash scripts/build-local.sh --skip-frontend
```
**Alias:** `bv` (both platforms, via `build-verify` in the respective `dev-aliases`)

---

### `build-service.ps1` — Build one service (internal helper)

Called by `build-profile`, `build-wealth`, etc. Not called directly by users.

```powershell
.\scripts\build-service.ps1 profile
.\scripts\build-service.ps1 wealth -NoCache
```
Services: `profile` | `wealth` | `health` | `household` | `gateway` | `shared` | `web`

---

### `dev-service.ps1` — Start one service in dev mode (internal helper)

Called by `dev-profile`, `dev-wealth`, etc. Opens a new terminal window with `quarkusDev` or `npm start`. Checks if the port is already in use before starting. Port/Gradle task come from `scripts/services.json`. Registers the service's real PID asynchronously once its port binds (see "PID-File Service Registry" above) — does not block the caller.

**Startup order matters:** profile → wealth/health/household → gateway → frontend.

---

### `test-service.ps1` — Run tests for one service (internal helper)

Runs all Gradle test tasks for a domain and writes output to `.temp/logs/<service>/test_<timestamp>.log`.

Called by `test-profile`, `test-wealth`, etc.

Gradle tasks per domain:
- `profile` → domain + ports + adapters
- `wealth` → domain + ports + adapters
- `health` → domain + ports + adapters
- `household` → domain + ports + adapters
- `gateway` → gateway tests
- `shared` → ArchUnit tests
- `web` → `npm run test:ci`

---

### `health-check.ps1` / `health-check.sh` — Service health report

HTTP GET to each backend service's real `/q/health` endpoint (quarkus-smallrye-health — see "Real
Health Checks" above) + TCP check for the database. Only HTTP 200 with body `status: "UP"` counts
as UP. Shows the PID-registry entry next to each service, if one exists (informational only).

```powershell
.\scripts\health-check.ps1
```
```bash
bash scripts/health-check.sh
```
**Alias:** `status` (both platforms — `dev-aliases.sh`'s `status` delegates to `health-check.sh` the same way `dev-aliases.ps1`'s does to `health-check.ps1`)

Endpoints (ports from `scripts/services.json`):
- profile: `http://localhost:8081/q/health`
- wealth: `http://localhost:8082/q/health`
- health: `http://localhost:8083/q/health`
- household: `http://localhost:8084/q/health`
- gateway: `http://localhost:8080/q/health`
- frontend: `http://localhost:3000` (plain reachability check — no `/q/health`, it's not a Quarkus service)

---

### `stop-all.ps1` — Kill all services

Checks the PID-file registry first for each service (see "PID-File Service Registry" above);
falls back to killing whatever process is listening on that service's port (ports 3000, 8080–8084
from `scripts/services.json`) only if no valid PID record exists.

```powershell
.\scripts\stop-all.ps1
```
**Alias:** `sa`

---

### `sonar-scan.ps1` — Run SonarQube analysis

Generates LCOV coverage (`npm run test:coverage`), runs `sonar-scanner`, then opens the dashboard in the browser.

```powershell
.\scripts\sonar-scan.ps1
```
**Alias:** `ss`

Requires: SonarQube running at `http://localhost:9000`. Start it with `sonar-start` if not running.

Requires: `$env:SONAR_TOKEN` set in the shell before running. `sonar-project.properties` is
git-tracked (despite being listed under `.gitignore`'s "Secrets" section — that entry predates
the commit and no longer has effect) and deliberately does **not** contain a token; the
`sonar-scanner` CLI reads `SONAR_TOKEN` from the environment automatically. Generate a token at
`http://localhost:9000/account/security` and set it once per shell session:
```powershell
$env:SONAR_TOKEN = "sqp_..."
```
The script also computes `sonar.java.libraries` / `sonar.java.test.libraries` from
`$env:GRADLE_USER_HOME` and passes them via `-D` flags, so no machine-specific path is
hardcoded in `sonar-project.properties`.

---

### `sonar-start.ps1` — Start SonarQube server

Finds the SonarQube installation in the repo root (`sonarqube-*/`) and starts the server.

```powershell
.\scripts\sonar-start.ps1
```
**Alias:** `sonar-start`

SonarQube takes ~60 seconds to be ready. Dashboard: `http://localhost:9000`.

---

### `db-reset.ps1` — Reset the database

Drops and recreates `app_db`, then runs `application/flyway/00_bootstrap.sql`. All data is lost.

```powershell
.\scripts\db-reset.ps1           # shows confirmation prompt
.\scripts\db-reset.ps1 -Force    # skip prompt
```
**Alias:** `db-reset`

After reset: start profile service first so it runs Flyway migrations on `app_db`.

---

### `db-shell.ps1` — Open psql shell

```powershell
.\scripts\db-shell.ps1            # connects as app_user to app_db
.\scripts\db-shell.ps1 -AsAdmin   # connects as postgres superuser
```
**Alias:** `db-shell`

---

### `db-start.ps1` — Ensure PostgreSQL is running

Checks if PostgreSQL Windows service is running, starts it if not, then opens `db-shell`.

```powershell
.\scripts\db-start.ps1
```
**Alias:** `db-start`

---

### `generate-api.ps1` — Regenerate TypeScript API client

Runs `npm run generate:api` in `web/`. Reads `application/contract/gateway.yaml` and writes `web/src/api/generated.ts`.

```powershell
.\scripts\generate-api.ps1
```
**Alias:** `gapi`

Run after any change to an OpenAPI contract file.

---

### `lnav.ps1` — Live log viewer

Opens lnav watching Quarkus service runtime logs. Services write to `~/.suchika/logs/` when running in dev mode.

```powershell
.\scripts\lnav.ps1               # all 5 services
.\scripts\lnav.ps1 wealth        # one service
.\scripts\lnav.ps1 wealth,health # two services
```
**Alias:** `lnav-dev`

Requires: `winget install tstack.lnav` (already installed). Custom format: `~/.lnav/formats/installed/suchika.json`.

Key lnav bindings: `e`/`E` (errors), `/` (search), `:filter-in pattern`, `;SQL query`, `q` (quit).

---

### `logs.ps1` — Tail build/test logs

Shows or tails the latest `.log` file from `.temp/logs/<service>/`.

```powershell
.\scripts\logs.ps1               # list newest log per service
.\scripts\logs.ps1 profile       # tail latest profile log
.\scripts\logs.ps1 wealth -Lines 100
```
**Alias:** `logs`

---

### `clean-builds.ps1` — Safe clean

Removes Gradle build outputs and React build artifacts. Does NOT touch `node_modules`, `.env`, or source files.

```powershell
.\scripts\clean-builds.ps1
```
**Alias:** `clean-builds`

---

### `clean-all.ps1` — Nuclear clean

Removes everything git doesn't track, including `node_modules` and `.gradle` cache. Always shows a dry-run preview first. Always preserves `application/finance/.env`.

```powershell
.\scripts\clean-all.ps1           # dry-run preview, then confirm
.\scripts\clean-all.ps1 -Force    # skip confirmation
```
**Alias:** `clean-all`

---

### `setup-dev.ps1` — First-time developer setup

Runs check-prerequisites, creates `application/finance/.env` from `infrastructure/local/.env.template` (creating the `application/finance/` directory first — it isn't git-tracked, so it doesn't exist on a fresh clone), bootstraps the database, installs npm dependencies.

```powershell
.\scripts\setup-dev.ps1
.\scripts\setup-dev.ps1 -SkipDb   # skip database steps (if already set up)
```
**Alias:** `setup-dev`

---

### `check-prerequisites.ps1` / `.sh` — Tool verification

Checks Java (floor/target from `scripts/services.json`'s `versionFloors` — currently 21+ warns, 25 matches CI/build.gradle.kts; below 21 fails, Java 17 is no longer supported), Gradle, Node (20+ warns, 24 matches CI), npm, psql, sonar-scanner. Reports PASS/WARN/FAIL per tool.

```powershell
.\scripts\check-prerequisites.ps1
```
**Alias:** `check`

---

### `check-migrations-location.sh` — Git pre-commit hook

Checks that no Flyway `.sql` files exist under `adapters/`. They must live in `application/flyway/<domain>/`.

Called automatically by pre-commit hook (`.husky/pre-commit`) and by CI's `migration-location-check` job. Not intended for manual use.

---

### `check-ps1-bom.sh` — Git pre-commit hook (new, 2026-07-13)

Checks that every `scripts/*.ps1` file is saved as UTF-8 with a byte-order mark — see "Encoding
note" below for why this matters. Mirrors `check-migrations-location.sh`'s pattern exactly (a
flat pass/fail check, no dependencies beyond coreutils).

```bash
bash scripts/check-ps1-bom.sh
```

Called automatically by `.husky/pre-commit` and by CI's `migration-location-check` job (same job
as the migration check, so a missing BOM fails the pipeline before any build step runs). Not
intended for manual use, though it's safe to run any time.

---

## Adding a New Script

1. Create `scripts/<name>.ps1` using the standard pattern:
   - `#Requires -Version 5.1` header
   - Comment block with Usage
   - `$root = Split-Path -Parent $PSScriptRoot`
   - Color helper functions: `Step`, `OK`, `Warn`, `Fail`
   - If the script needs a port, DB schema, Gradle task, the DB password fallback, or a version
     floor: `. "$PSScriptRoot\config.ps1"` and read it from `$SuchikaServices` / `$SuchikaDb` /
     `$SuchikaVersionFloors` — **do not hardcode a new copy of a value already in
     `scripts/services.json`.**
   - **Save the file as UTF-8 with a BOM** (all `.ps1` files in this repo carry one — see
     "Encoding note" below). This is no longer just a convention: `check-ps1-bom.sh` enforces it
     on every commit via `.husky/pre-commit` and in CI. If you author a `.ps1` file with a tool
     that doesn't add a BOM by default (this includes most text editors and the Write tool many
     coding agents use), re-save it explicitly before committing:
     ```powershell
     $c = Get-Content -Raw -Path scripts\<name>.ps1
     [IO.File]::WriteAllText("scripts\<name>.ps1", $c, (New-Object Text.UTF8Encoding($true)))
     ```
2. If user-facing: add a function to `dev-aliases.ps1` in the appropriate section
3. If it needs a short alias: add `Set-Alias` to the aliases section
4. Update `help-dev` table in `dev-aliases.ps1`
5. Update this document (`documents/SCRIPTS.md`) with a new entry
6. If you add a `.ps1` script with no bash equivalent (or vice versa), state explicitly in this
   document *why* — "intentional, environment-driven" is only a real answer if the reason is
   written down next to it (see the `dev-aliases.sh` "Behavioral differences" list above for the
   expected level of detail). An undocumented gap looks identical to a forgotten port.

### Encoding note (Windows PowerShell 5.1 gotcha)

Windows PowerShell 5.1 (`powershell.exe`, not `pwsh.exe`) decodes a BOM-less `.ps1` file using the system's ANSI codepage, not UTF-8. Any non-ASCII character sitting inside an actual string literal (an em dash `—`, an arrow `→`/`←`, etc. — box-drawing characters and other decoration are safe *only* inside `#` comments or single-quoted here-strings `@'...'@`) gets mis-decoded and can silently corrupt string/quote parsing for the rest of the file. All 19 `.ps1` scripts in this repo were re-saved with a UTF-8 BOM (2026-07-06) to close this off permanently; keep new scripts saved the same way (PowerShell 5.1's own `Set-Content -Encoding UTF8` / ISE's default save both add the BOM automatically). PowerShell 7+ (`pwsh`) and every `.sh` script are unaffected — don't add a BOM to `.sh` files, it breaks the `#!` shebang line.

### Removed: `scripts/documentWriter.py` (2026-07-13)

A Python script previously lived in `scripts/` that staged, classified, and merged stray `.md`
files into the canonical `documents/*.md` files and rewrote the README's repository-tree section.
It was not part of the documented script set above, had no safety/dry-run gate, and its own
docstring pointed at a `tools/documentWriter.py` path that never existed in this repo. Evidence
found during the 2026-07-06 retrospective suggested a keyword-classification bug in it had already
merged unrelated agent/command-definition content into `documents/GETTING_STARTED.md`. It has been
deleted outright rather than fixed: the `document-writer` subagent (see `.claude/agents/`) already
covers documentation consolidation with proper judgment instead of keyword matching, so there was
no gap left to fill.

---

## Log Directory Layout

```
<repo-root>/.temp/logs/          ← build and test logs (gitignored)
    profile/test_2024-01-01.log
    wealth/test_2024-01-01.log
    ...

~/.suchika/logs/                  ← runtime service logs (outside repo)
    profile.log
    wealth.log
    health.log
    household.log
    gateway.log
    web.log                        (bash/Codespaces only — Windows dev mode opens a window instead)

~/.suchika/run/                   ← PID-file service registry (outside repo, added 2026-07-13)
    profile.pid                    {"pid": ..., "processName": ..., "port": ..., "service": ..., "startedAt": ...}
    wealth.pid
    ...                            one file per running service; see "PID-File Service Registry" above
```

Both directories are configurable via `scripts/services.json`'s `logDir`/`pidDir` fields (default
`~/.suchika/logs` and `~/.suchika/run`).
