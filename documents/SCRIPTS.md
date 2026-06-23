# Scripts Reference

| | |
|---|---|
| **Type** | Reference |
| **Audience** | Developers |
| **Status** | Active |
| **Last updated** | 2026-06-23 |

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
| Start all services | `. .\scripts\dev-aliases.ps1` then `da` |
| Start one service | `dp` / `dw` / `dh` / `dho` / `dg` / `dwb` |
| Run tests for a domain | `tp` / `tw` / `tsa` |
| Full pre-commit check | `bv` |
| Check if services are up | `status` |
| Stop everything | `sa` |
| Watch logs live | `lnav-dev` |
| Tail build/test log | `logs [service]` |
| Open database shell | `db-shell` |
| Reset database | `db-reset` (destructive) |
| Run SonarQube | `ss` |
| Regenerate API client | `gapi` |
| First-time setup | `setup-dev` |

---

## Script Catalogue

### `dev-aliases.ps1` — Primary developer interface
Dot-source once per terminal session. Defines all short aliases and functions.

```powershell
. .\scripts\dev-aliases.ps1
help-dev    # print full alias list
```

**Build aliases:** `bp` `bw` `bh` `bho` `bg` `bwb` `ba` `bv`
**Dev aliases:** `dp` `dw` `dh` `dho` `dg` `dwb` `da`
**Test aliases:** `tp` `tw` `tsa`
**Quality:** `ss` (sonar-scan) `gapi` (generate-api)
**Control:** `sa` (stop-all) `status` `check`
**Database:** `db-start` `db-reset` `db-shell`
**Logs:** `logs` `lnav-dev`
**Maintenance:** `clean-builds` `clean-all` `setup-dev`

---

### `build-local.ps1` — Full pre-commit verification

Builds all services in dependency order, runs ArchUnit, all tests, lint, Prettier, and optionally SonarQube.

```powershell
.\scripts\build-local.ps1                  # full verification
.\scripts\build-local.ps1 -SkipSonar       # skip SonarQube (faster)
.\scripts\build-local.ps1 -SkipFrontend    # backend only
.\scripts\build-local.ps1 -FrontendOnly    # frontend only
```
**Alias:** `bv`

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

Called by `dev-profile`, `dev-wealth`, etc. Opens a new terminal window with `quarkusDev` or `npm start`. Checks if the port is already in use before starting.

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

### `health-check.ps1` — Service health report

HTTP GET to each service's OpenAPI endpoint + TCP check for the database.

```powershell
.\scripts\health-check.ps1
```
**Alias:** `status`

Expected endpoints:
- profile: `http://localhost:8081/q/openapi`
- wealth: `http://localhost:8082/q/openapi`
- health: `http://localhost:8083/q/openapi`
- household: `http://localhost:8084/q/openapi`
- gateway: `http://localhost:8080/q/openapi`
- frontend: `http://localhost:3000`

---

### `stop-all.ps1` — Kill all services

Kills processes listening on ports 3000, 8080–8084.

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

Runs check-prerequisites, creates `.env` from template, bootstraps the database, installs npm dependencies.

```powershell
.\scripts\setup-dev.ps1
.\scripts\setup-dev.ps1 -SkipDb   # skip database steps (if already set up)
```
**Alias:** `setup-dev`

---

### `check-prerequisites.ps1` / `.sh` — Tool verification

Checks Java 17+, Gradle, Node 18+, npm, psql, sonar-scanner. Reports PASS/WARN/FAIL per tool.

```powershell
.\scripts\check-prerequisites.ps1
```
**Alias:** `check`

---

### `check-migrations-location.sh` — Git pre-commit hook

Checks that no Flyway `.sql` files exist under `adapters/`. They must live in `application/flyway/<domain>/`.

Called automatically by pre-commit hook. Not intended for manual use.

---

## Adding a New Script

1. Create `scripts/<name>.ps1` using the standard pattern:
   - `#Requires -Version 5.1` header
   - Comment block with Usage
   - `$root = Split-Path -Parent $PSScriptRoot`
   - Color helper functions: `Step`, `OK`, `Warn`, `Fail`
2. If user-facing: add a function to `dev-aliases.ps1` in the appropriate section
3. If it needs a short alias: add `Set-Alias` to the aliases section
4. Update `help-dev` table in `dev-aliases.ps1`
5. Update this document (`documents/SCRIPTS.md`) with a new entry

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
```
