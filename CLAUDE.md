# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Agent Bootstrap Protocol

Before starting any task, read these two files — they give complete project context in ~3 minutes:

1. **[documents/CONTEXT_PRIMER.md](documents/CONTEXT_PRIMER.md)** — current version, service map, key invariants, what's done
2. **[documents/domain-state/\<domain\>.md](documents/domain-state/)** — schema, ADRs, open issues for the domain being touched

After completing any task, update the relevant `documents/domain-state/<domain>.md`.

Use domain-specific agents when available: `wealth-developer`, `health-developer`, `profile-developer`, `household-developer` — they have domain context pre-loaded.

---

## Commands

### Backend
```bash
# Start domain services (hot reload). Run profile FIRST — other domains FK into profile.profile.
# Port assignments: profile=8081, wealth=8082, health=8083, household=8084, gateway=8080
./gradlew :application:domain:profile:adapters:quarkusDev
./gradlew :application:domain:wealth:adapters:quarkusDev
./gradlew :application:domain:health:adapters:quarkusDev
./gradlew :application:domain:household:adapters:quarkusDev
./gradlew :application:web-gateway:quarkusDev   # BFF aggregator; frontend talks only to this

# Run all tests
./gradlew test

# Run tests for a specific module
./gradlew :application:domain:wealth:domain:test

# Clean build
./gradlew clean
```

### Frontend (run from `web/`)
```bash
npm install
npm run generate:api   # Regenerate typed API client — run after any contract change
npm start              # Dev server at http://localhost:3000
npm run lint
npm run lint:fix       # Auto-fix ESLint errors
npm run format
npm run test:ci        # Single-run (non-watch)
```

### One-time database setup (before first run)
```bash
psql -U postgres -c "CREATE DATABASE app_db;"
psql -U postgres -d app_db -f application/flyway/00_bootstrap.sql
cp infrastructure/local/.env.template application/finance/.env
# Edit application/finance/.env and set DB_PASSWORD
```

## Architecture

Suchika is a personal household management system using **Hexagonal Architecture (Ports & Adapters)** with four domains, each a Gradle sub-project:

```
application/domain/
├── profile/    ← identity anchor; every other domain FKs into profile.profile
├── wealth/     ← financial accounts, transactions (CQRS write model), physical assets
├── household/  ← calendar events, grocery inventory, goals
└── health/     ← vital readings (weight, BP, blood sugar), doctor visits
```

Each domain has three layers:
- `domain/` — pure Java business logic, zero framework dependencies (no `@Inject`, no JPA, no HTTP)
- `ports/` — interface contracts: input ports (use cases), output ports (persistence)
- `adapters/` — HTTP controllers and Panache/JPA persistence implementations

Package convention: `com.suchika.{domain}.domain.*` / `.ports.input.*` / `.ports.output.*` / `.adapters.*`

`web-gateway` is a Backend-for-Frontend (BFF) aggregator for cross-domain dashboard data. It has no database dependency — it composes domain REST calls via MicroProfile Rest Client and runs CQRS projections. Domain service contracts (`application/contract/{domain}.yaml`) are mirrored into `application/web-gateway/src/main/resources/` for the Rest Client. The gateway contract (`application/contract/gateway.yaml`) is what the frontend generates its typed client from.

**Frontend talks only to the gateway at `http://localhost:8080`** — never to domain services directly. Domain service Swagger UIs are available at `http://localhost:{port}/swagger-ui`.

## Database Structure

**Single PostgreSQL database (`app_db`) with five schemas:**

| Schema | Owner | Content |
|---|---|---|
| `profile` | profile module | `admin` — household manager; `profile` — all household members |
| `wealth` | wealth module | `account`, `transaction`, `statement_upload`, `upload_error_log`, `physical_asset` |
| `household` | household module | `calendar_event`, `inventory_item`, `goal` |
| `health` | health module | `vital_reading`, `doctor_visit` |
| `projections` | web-gateway | `dashboard_snapshot` — CQRS read model (UPSERT on recalculation) |

Profile schema design: `profile.admin` is the household manager (future auth anchor). `profile.profile` holds all household members and has an `admin_id FK → profile.admin`. Every other domain's tables hold `profile_id UUID REFERENCES profile.profile(id)` — pointing into the member record, never into admin. No cross-domain SQL joins.

## Migrations

Flyway migrations live in `application/flyway/{domain}/` and are auto-run by each module on startup.

- `application/flyway/00_bootstrap.sql` — run **manually** once as superuser before any module starts; creates schemas and roles
- Each domain starts at `V1__` — never skip or edit a committed migration; create a new versioned file
- **Startup order matters:** profile must run first (other modules' migrations reference `profile.profile`)

## Dashboard Projections (CQRS)

Calculations run once and are stored in `projections.dashboard_snapshot` via UPSERT:
```sql
INSERT INTO projections.dashboard_snapshot (profile_id, snapshot_key, payload)
VALUES (:profileId, 'WEALTH_NET_WORTH', :jsonPayload)
ON CONFLICT (profile_id, snapshot_key) DO UPDATE
  SET payload = EXCLUDED.payload, calculated_at = now();
```
The calculation engine lives in `web-gateway` (the BFF), which has read access to all domain schemas.

## Key Rules

**Domain layer:** `domain/` must have zero framework dependencies. No `@Inject`, no JPA annotations (`jakarta.persistence.*`), no HTTP types. Enforced by ArchUnit in `shared/src/test/java/.../DomainRulesTest.java` — read that file before writing new classes; it documents the full dependency rules including cross-domain isolation and logging requirements.

**Flyway:** Never edit a committed migration — create a new versioned file. `00_bootstrap.sql` is manual-only (Flyway does not run it). Exception: each domain's Flyway history was consolidated into a single `V1__init_<domain>_consolidated.sql` (2026-07-04/05, product-owner-approved override of this rule for the pre-release consolidation only — requires a manual dev DB reset, `DROP SCHEMA ... CASCADE` + re-migrate). This exception does not reopen the rule generally — once consolidated, V1 is committed again and the normal "never edit" rule resumes.

**DB constraint philosophy — two categories (revised 2026-07-05, supersedes the prior CHECK-constraint guidance):**
- **Keep in DB** (structural invariants enforced everywhere, including direct DB access): NOT NULL, PK, FK, UNIQUE.
- **Do NOT add to DB — no CHECK constraints of any kind**, not just enum discriminators. This now also covers business-rule checks previously kept in DB (`amount >= 0`, `end_date >= start_date`, `visited_doctor = TRUE → doctor_name NOT NULL`, `target_amount > 0`, `current_amount >= 0`, etc.). All of these move to the domain layer (a validating static factory method, e.g. `Goal.create(...)`, throwing `IllegalArgumentException`) plus the OpenAPI contract where applicable. Adding or changing a rule requires only a domain/contract + code change — no Flyway migration needed.
- **VARCHAR name columns are capped at `VARCHAR(50)`** (e.g. `account_name`, `institution_name`, `asset_name`, `display_name`, `full_name`) — a project-wide standard, not per-domain judgment calls.

**No SQL ENUMs ever.** Use plain `VARCHAR` with no CHECK constraint for discriminator columns.

**Timestamps are always IST (UTC+5:30):** All `TIMESTAMPTZ` columns use `Asia/Kolkata` enforced at two levels — `ALTER DATABASE app_db SET timezone = 'Asia/Kolkata'` in `00_bootstrap.sql`, and `quarkus.hibernate-orm.jdbc.timezone=Asia/Kolkata` in every service's `application.properties`. Never store or read timestamps in any other timezone. New services must include both settings.

**Profile-scoped isolation (ADR-006):** Every DB query across all domains must filter by the active `profile_id`. This filter is injected in the `adapters/` layer — never in `domain/` or `ports/`.

**API client:** After any backend contract change, regenerate with `cd web && npm run generate:api`. Never hand-edit `web/src/api/generated.ts`. The frontend API base URL is `REACT_APP_API_BASE_URL` (defaults to `http://localhost:8080`).

**Frontend structure:** Pages live in `src/pages/Public/` (no auth), `src/pages/User/` (user + admin), `src/pages/Admin/` (admin only). Wrap protected routes in `<ProtectedRoute requiredRole="admin">`. Use `useAuth()` for role checks. Route paths are domain-segmented: `/wealth`, `/household`, `/health`. API calls go in custom hooks or `useEffect` — never in component render.

**Logging & exceptions:** Use `AppLogger` from `shared/` for all logging. Throw typed exceptions from `shared/exception/` hierarchy (`NotFoundException`, `BadRequestException`, etc.) — `ApplicationExceptionMapper` converts them to HTTP responses automatically.

**Styling:** Tailwind CSS only in the frontend. No CSS modules, no inline `style={{}}`, no other CSS frameworks.

## GitHub Codespaces

This repo is Codespaces-ready. The `.devcontainer/` directory configures a two-container environment:
- **app** — Java 17 + Node 18 + PowerShell Core (for `.ps1` scripts)
- **db** — PostgreSQL 16 in IST timezone

**First time in Codespaces:**
1. Open the repo in Codespaces — `setup.sh` runs automatically and bootstraps the database
2. Open a terminal and run: `. ./scripts/dev-aliases.sh` (or reload the terminal — it auto-loads)
3. Type `help-dev` to see all commands
4. Run `dp` (start profile service first), then `dw`, `dh`, `dg`, `dwb`

**Key Codespaces differences vs Windows:**
- Services run in background (no new terminal windows); watch via `lnav-dev`
- `DB_URL` env var is set to `db:5432` (the PostgreSQL container name); no `.env` change needed
- Use `scripts/dev-aliases.sh` (not `.ps1`) — the bash version is auto-loaded in Codespaces terminals
- SonarQube is not available in Codespaces (too resource-intensive for free tier); run `ss` locally before PRs
- Port forwarding: VS Code shows forwarded ports in the Ports panel; use the public URL for browser access

**Prebuilds:** The `onCreateCommand` in `devcontainer.json` downloads all Gradle and npm dependencies during prebuild, so new codespaces are ready in ~30 seconds instead of ~5 minutes. Enable prebuilds in GitHub repo settings under Codespaces → Prebuilds.

## Key Documentation

- [documents/ARCHITECTURE_GUIDELINES.md](documents/ARCHITECTURE_GUIDELINES.md) — architectural rules enforced on every PR
- [documents/BUSINESS_REQUIREMENTS.md](documents/BUSINESS_REQUIREMENTS.md) — feature specs and milestone roadmap
- [documents/FRONTEND_GUIDELINES.md](documents/FRONTEND_GUIDELINES.md) — React/Tailwind/ESLint standards
- [documents/LOGGING_AND_EXCEPTIONS.md](documents/LOGGING_AND_EXCEPTIONS.md) — shared logger and exception usage
- [documents/SCRIPTS.md](documents/SCRIPTS.md) — all scripts in scripts/ with parameters and aliases
