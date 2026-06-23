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

## Branch & PR Governance Workflows

Four additional workflows and two config files run on every pull request (not on direct pushes). **All are required checks — a failing check blocks the PR from merging.**

---

### `branch-name-check.yml`

Validates the source branch name before any code is reviewed.

**Required pattern:** `<type>/<description>`

| Segment | Rule |
|---|---|
| `type` | One of: `feat`, `fix`, `docs`, `refactor`, `chore`, `test`, `ci`, `hotfix`, `perf` |
| `description` | Lowercase letters, numbers, and hyphens only — no uppercase, no underscores, no slashes |

**Valid examples:**
```
feat/wealth-csv-upload
fix/health-negative-weight-validation
ci/add-branch-enforcement
hotfix/profile-null-pointer
```

**What happens on failure:** the workflow prints the rejected branch name and the required format, then exits 1. You must create a new branch with a conforming name — you cannot rename an existing branch after a PR is open.

---

### `pr-title-lint.yml`

Enforces [Conventional Commits](https://www.conventionalcommits.org/) format on the PR title. Runs again whenever the title is edited.

**Required pattern:** `<type>(<optional-scope>): <description>` (max 72 chars on the description)

| Field | Allowed values |
|---|---|
| `type` | `feat`, `fix`, `docs`, `refactor`, `chore`, `test`, `ci`, `hotfix`, `perf` |
| `scope` (optional) | `profile`, `wealth`, `health`, `household`, `gateway`, `web`, `ci`, `shared` |

**Valid examples:**
```
feat(wealth): add CSV upload for bank statements
fix(health): reject negative weight readings at API boundary
docs: add ADR-007 for cross-domain event model
chore(ci): add SonarCloud analysis as required check
ci: add branch name enforcement workflow
```

**What happens on failure:** the workflow prints the rejected title and shows the expected format. Fix by editing the PR title directly on GitHub — no new commit needed.

---

### `pr-labeler.yml`

Auto-labels PRs using the path-to-label mapping in `.github/labeler.yml`. Labels are cosmetic and informational; they do not block merging.

Example mappings:
- Changes under `application/domain/health/` → `health` label
- Changes under `web/src/` → `frontend` label
- Changes under `.github/workflows/` → `ci` label

---

### `CODEOWNERS`

`.github/CODEOWNERS` defines required reviewers per path. When branch protection is active, GitHub enforces that the designated code owner(s) must approve before the PR can be merged. If you are unsure who owns a path, check that file directly.

---

## Key Rules

- Never edit a committed Flyway migration — always add a new versioned file.
- Always run `npm run generate:api` after any backend contract change.
- All domain services use the same PostgreSQL database (`app_db`) with separate schemas per domain.
- No cross-domain SQL joins, ever.
- CI triggers on `main` branch only — `master` trigger was removed.
- Branch names must match `<type>/<description>` — allowed types: `feat`, `fix`, `docs`, `refactor`, `chore`, `test`, `ci`, `hotfix`, `perf`.
- PR titles must follow Conventional Commits format — same type list, optional scope from the allowed scope list.
- See [CONTRIBUTING.md](../CONTRIBUTING.md) for the full branch/PR quick-reference with examples.
