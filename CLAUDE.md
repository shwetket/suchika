# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

### Backend
```bash
# Run a domain module in dev mode (hot reload, port 8080)
# Run profile FIRST — other domains FK into profile.profile
./gradlew :application:domain:profile:adapters:quarkusDev
./gradlew :application:domain:wealth:adapters:quarkusDev
./gradlew :application:domain:health:adapters:quarkusDev
./gradlew :application:domain:household:adapters:quarkusDev

# Run all tests
./gradlew test --continuous=false

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

`web-gateway` is a Backend-for-Frontend (BFF) aggregator for cross-domain dashboard data. It has no database dependency — it composes domain REST calls and runs CQRS projections.

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

**Flyway:** Never edit a committed migration — create a new versioned file. `00_bootstrap.sql` is manual-only (Flyway does not run it).

**DB constraint philosophy — two categories:**
- **Keep in DB** (structural invariants enforced everywhere, including direct DB access): NOT NULL, PK, FK, UNIQUE, and business-rule checks like `amount >= 0`, `end_date >= start_date`, `visited_doctor = TRUE → doctor_name NOT NULL`.
- **Do NOT add to DB** (enum discriminators — values that form a list of allowed strings): account types, event types, vital types, relation values, status codes, platform names. These are enforced at the OpenAPI contract (enum on the schema) and Java enum + `@Valid` annotation. Adding a new value requires only a contract + code change — no Flyway migration needed.

**No SQL ENUMs ever.** Use plain `VARCHAR` with no CHECK constraint for discriminator columns.

**API client:** After any backend contract change, regenerate with `cd web && npm run generate:api`. Never hand-edit `web/src/api/generated.ts`.

**Logging & exceptions:** Use `AppLogger` from `shared/` for all logging. Throw typed exceptions from `shared/exception/` hierarchy (`NotFoundException`, `BadRequestException`, etc.) — `ApplicationExceptionMapper` converts them to HTTP responses automatically.

**Styling:** Tailwind CSS only in the frontend. No CSS modules, no inline `style={{}}`, no other CSS frameworks.

## Key Documentation

- [documents/ARCHITECTURE_GUIDELINES.md](documents/ARCHITECTURE_GUIDELINES.md) — architectural rules enforced on every PR
- [documents/BUSINESS_REQUIREMENTS.md](documents/BUSINESS_REQUIREMENTS.md) — feature specs and milestone roadmap
- [documents/FRONTEND_GUIDELINES.md](documents/FRONTEND_GUIDELINES.md) — React/Tailwind/ESLint standards
- [documents/LOGGING_AND_EXCEPTIONS.md](documents/LOGGING_AND_EXCEPTIONS.md) — shared logger and exception usage
