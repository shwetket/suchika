# Contributing & Getting Started

This file covers local development setup, prerequisites, and run commands.

---

## Prerequisites

| Tool | Version | Purpose |
|---|---|---|
| Java | 17 | Backend runtime |
| Gradle wrapper | (bundled) | No separate install needed |
| Node.js | 18+ | Frontend |
| PostgreSQL | 14+ | All domains (single instance) |

No MongoDB required. All domains use PostgreSQL via a schema-per-domain architecture.

---

## Quick Start (Windows / PowerShell)

Load the developer aliases once per terminal session, then use short commands for everything:

```powershell
# From repo root — activate all dev aliases
. .\scripts\dev-aliases.ps1

# First-time setup (DB + .env + npm install)
setup-dev

# Start all services (opens separate windows; profile starts FIRST)
da          # short for dev-all

# Check all services are UP
status
```

Type `help-dev` after loading the aliases to see the full command reference.

### Key aliases

| Alias | Full name | Action |
|---|---|---|
| `dp` | `dev-profile` | Start profile service (port 8081) — always first |
| `da` | `dev-all` | Start all 6 services in dependency order |
| `ba` | `build-all` | Build everything (Gradle cache on) |
| `bv` | `build-verify` | Full pre-commit check: no-cache + tests + Sonar |
| `tsa` | `test-all` | Run all backend tests |
| `ss` | `sonar-scan` | Analyse + open dashboard |
| `sa` | `stop-all` | Kill all services |
| `gapi` | `generate-api` | Regenerate `web/src/api/generated.ts` |
| `status` | — | Show HTTP/TCP health of all services |

---

## One-Time Database Setup

### Option A — via script

```powershell
. .\scripts\dev-aliases.ps1
setup-dev
```

### Option B — manually

```bash
# Connect as superuser and create the database
psql -U postgres -c "CREATE DATABASE app_db;"

# Run the bootstrap (creates schemas, app_user, and grants)
psql -U postgres -d app_db -f application/flyway/00_bootstrap.sql
```

Then copy and edit the environment file:

```bash
cp infrastructure/local/.env.template application/finance/.env
```

`application/finance/.env`:
```properties
DB_URL=jdbc:postgresql://localhost:5432/app_db
DB_USERNAME=app_user
DB_PASSWORD=your_secure_password_here
```

The bootstrap creates five schemas (`profile`, `wealth`, `household`, `health`, `projections`)
and a single `app_user` role used by all modules in local dev.

---

## Running the Backend

Each domain is an independent Quarkus module. Run in separate terminals.
Flyway migrations run automatically on startup — schemas are populated on first run.

> **Profile MUST start first** — other domains FK into `profile.profile`.

### Via aliases (recommended)

```powershell
dp    # profile  → http://localhost:8081
dw    # wealth   → http://localhost:8082
dh    # health   → http://localhost:8083
dho   # household→ http://localhost:8084
dg    # gateway  → http://localhost:8080
```

### Via Gradle directly

```bash
./gradlew :application:domain:profile:adapters:quarkusDev    # 8081
./gradlew :application:domain:wealth:adapters:quarkusDev     # 8082
./gradlew :application:domain:health:adapters:quarkusDev     # 8083
./gradlew :application:domain:household:adapters:quarkusDev  # 8084
./gradlew :application:web-gateway:quarkusDev                # 8080 (BFF)
```

### Ports

| Service | Port | Notes |
|---|---|---|
| web-gateway (BFF) | 8080 | Aggregates domain services; Swagger UI here |
| profile | 8081 | Identity anchor — start first |
| wealth | 8082 | |
| health | 8083 | |
| household | 8084 | |
| React dev server | 3000 | `npm start` or `dwb` |
| PostgreSQL | 5432 | |
| SonarQube (local) | 9000 | |

- Swagger UI: `http://localhost:8080/swagger-ui`
- OpenAPI JSON: `http://localhost:8080/q/openapi`

---

## Running the Frontend

```bash
cd web
npm install
npm run generate:api   # Regenerate typed API client from OpenAPI spec
npm start              # http://localhost:3000
```

Or via alias: `dwb` (opens a new terminal window).

Run `npm run generate:api` (or `gapi`) every time the backend API contract changes.

---

## Tests

```powershell
# All backend tests
tsa                    # alias → ./gradlew test

# Single domain
test-profile           # or: test-wealth, test-health, test-household, test-gateway
./gradlew :application:domain:wealth:domain:test

# Frontend tests
test-web               # npm run test:ci (single-run)
cd web && npm run test:ci
```

---

## Pre-Commit Verification

Before committing, run the full verification build:

```powershell
bv    # build-verify
```

This runs (via `scripts/build-local.ps1`):
1. `./gradlew clean build --no-build-cache` — backend tests + ArchUnit
2. `npm run lint && npm run test:ci` — frontend lint + tests
3. `sonar-scanner` — full Sonar analysis (starts server if needed)

Any failure stops the pipeline. Fix and rerun before committing.

---

## Code Quality: SonarQube

### Setup (one-time)

1. Download SonarQube Community Edition and extract it locally.
2. Start SonarQube:
   ```powershell
   sonar-start    # starts server + opens http://localhost:9000
   ```
3. Login (`admin` / `admin`), create a project key `suchika`, generate a token.
4. Add your token to `sonar-project.properties` (this file is gitignored):
   ```properties
   sonar.login=YOUR_TOKEN
   ```
5. Install sonar-scanner globally: `npm install -g sonar-scanner`

### Run analysis

```powershell
ss    # sonar-scan — builds + analyses + opens dashboard
```

---

## Migration Rules

Flyway migrations are in `application/flyway/{domain}/` and run automatically on module startup.

- **Never edit a committed migration file.** Create a new versioned file instead.
- `00_bootstrap.sql` is run manually once. Flyway does not manage it.
- Migration startup order: **profile → wealth → household → health**

---

## Typical Inner-Loop Workflow

```powershell
dp              # 1. Start profile (ALWAYS first)
da              # 2. Start all remaining services
status          # 3. Confirm all UP
# ... edit code ...
bp              # 4. Rebuild changed service (e.g. bp for profile)
tsa             # 5. Run all tests
ss              # 6. Sonar scan
bv              # 7. Full pre-commit check before pushing
```

---

## Troubleshooting

| Issue | Solution |
|---|---|
| Backend cannot connect to PostgreSQL | Check `application/finance/.env` and PostgreSQL service. Run `db-start`. |
| Flyway migration error on startup | Never edit a previous migration — create a new versioned file |
| `npm install` fails | Run `npm install --legacy-peer-deps` in `web/` |
| API client out of sync | Run `gapi` (or `cd web && npm run generate:api`) |
| Gradle compile fails | Run `clean-builds` then retry |
| Profile module Flyway fails | Ensure `00_bootstrap.sql` was run first (`setup-dev` does this) |
| Port already in use | Run `stop-all` (or `sa`) then `dev-all` |
| Aliases not found | Dot-source the file: `. .\scripts\dev-aliases.ps1` |
