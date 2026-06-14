# CI/CD

## Pipeline Overview

CI runs on every push and pull request via GitHub Actions (`.github/workflows/ci.yml`).
Three sequential jobs — migration check → backend → frontend.

---

## CI Jobs

### 1. `migration-location-check`

```bash
bash scripts/check-migrations-location.sh
```

Verifies no Flyway migration files exist inside `*/adapters/**/db/migration/`. All migrations must live in `application/flyway/{domain}/`. Fails the entire pipeline if violated.

### 2. `backend` (needs: migration-location-check)

```bash
./gradlew test --continuous=false
```

Runs all JUnit and ArchUnit tests across every module. Requires Java 21 (Temurin).
ArchUnit enforces domain layer purity — any `@Inject`, JPA annotation, or HTTP type inside `domain/` fails here.

### 3. `frontend` (needs: backend)

```bash
cd web
npm install
npm run generate:api      # Regenerate typed client from application/contract/gateway.yaml
npm run lint              # ESLint on src/ (*.js, *.jsx)
npm run format:check      # Prettier check on src/ and public/
npm run test:ci           # Jest, single run, no watch
npm run build             # Production React build
```

Requires Node.js 24.

---

## Module Reference

| Gradle module | Domain | HTTP port |
|---|---|---|
| `:application:domain:profile:adapters` | Profile (identity anchor) | 8081 |
| `:application:domain:wealth:adapters` | Wealth (accounts, transactions, assets) | 8082 |
| `:application:domain:health:adapters` | Health (vitals, doctor visits) | 8083 |
| `:application:domain:household:adapters` | Household (calendar, inventory, goals) | 8084 |
| `:application:web-gateway` | BFF — aggregates domain REST calls | 8080 |
| `web/` (React) | Frontend | 3000 |
| PostgreSQL | Shared DB, schema-per-domain | 5432 |

> Profile **must start first** — all other domain modules have Flyway migrations that reference `profile.profile`.

---

## OpenAPI Contracts

| Contract file | Serves | Port |
|---|---|---|
| `application/contract/profile.yaml` | Profile service | 8081 |
| `application/contract/wealth.yaml` | Wealth service | 8082 |
| `application/contract/health.yaml` | Health service | 8083 |
| `application/contract/household.yaml` | Household service | 8084 |
| `application/contract/gateway.yaml` | Web gateway (BFF) | 8080 |

After any contract change, regenerate the frontend client:
```bash
cd web && npm run generate:api
```
The generated file is `web/src/api/generated.ts`. Never hand-edit it.

---

## Local Pre-Commit Verification

Run before every commit to catch the same failures CI catches:

```bash
# Git Bash
bash scripts/build-local.sh

# PowerShell
.\scripts\build-local.ps1
```

This script mirrors CI exactly:
1. Migration location check
2. `./gradlew test` (all JUnit + ArchUnit)
3. `npm install`
4. `npm run generate:api`
5. `npm run format` (auto-fix — CI does format:check; local script fixes in place)
6. `npm run lint`
7. `npm run test:ci`
8. `npm run build`

---

## Environment Variables

| Variable | Used by | Description |
|---|---|---|
| `DB_URL` | All domain services | PostgreSQL JDBC URL (default: `jdbc:postgresql://localhost:5432/app_db`) |
| `DB_USERNAME` | All domain services | DB user (default: `app_user`) |
| `DB_PASSWORD` | All domain services | DB password |
| `PROFILE_SERVICE_URL` | Web Gateway | Override profile service base URL (default: `http://localhost:8081`) |
| `WEALTH_SERVICE_URL` | Web Gateway | Override wealth service base URL (default: `http://localhost:8082`) |
| `HEALTH_SERVICE_URL` | Web Gateway | Override health service base URL (default: `http://localhost:8083`) |
| `HOUSEHOLD_SERVICE_URL` | Web Gateway | Override household service base URL (default: `http://localhost:8084`) |

Set `DB_PASSWORD` and any non-default values in `application/finance/.env` (copied from `infrastructure/local/.env.template`).
In CI, set DB credentials as GitHub Actions Secrets.

---

## Key Rules

- Never edit a committed Flyway migration — always add a new versioned file.
- Always run `npm run generate:api` after any backend contract change.
- All domain services use the same PostgreSQL database (`app_db`) with separate schemas per domain.
- No cross-domain SQL joins, ever.
