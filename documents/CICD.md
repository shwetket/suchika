# CICD

## Pipeline Purpose

This file documents the automation jobs and technical pipeline rules for the Suchika project.
The pipeline covers three backend modules (Wealth, Health, Household), one React frontend, and pre-commit enforcement.

---

## Backend Pipeline

### Compile all three domain modules

```bash
# Wealth domain
./gradlew :application:finance:compileJava

# Health domain
./gradlew :application:health:compileJava

# Household domain (module name: records)
./gradlew :application:records:compileJava
```

> Note: `:application:records` is the Household domain backend module.
> The module is named `records` because it handles the household records system (profiles, calendar, inventory).

### Run backend tests

```bash
./gradlew test --continuous=false
```

Run tests for all modules. Tests are required to pass before any commit merges.

### Verify Flyway migrations

```bash
./gradlew :application:finance:quarkusDev
```

Flyway runs automatically on startup. If any migration fails, the build is considered broken.
Never edit a previously committed migration file — always add a new versioned file.

### Verify MongoDB connection (Health domain)

```bash
./gradlew :application:health:quarkusDev
```

On startup, the Health module attempts a connection to MongoDB at `MONGO_URL`.
If the connection fails, the build is considered broken.
Ensure MongoDB is running on port `27017` in the CI environment.

---

## Frontend Pipeline

### Install dependencies

```bash
cd web
npm install
```

### Generate the OpenAPI client

Run after any backend or contract change:

```bash
cd web
npm run generate:api
```

This regenerates the typed API client in `web/src/api/generated/` from the live backend spec.

### Build the production frontend

```bash
cd web
npm run build
```

---

## API Contract Synchronization

Three OpenAPI contract files must stay in sync with the backend:

| Contract File | Domain |
|---|---|
| `openapi/finance.yaml` | Wealth |
| `openapi/health.yaml` | Health |
| `openapi/household.yaml` | Household |

**Rules:**
- When any OpenAPI file changes, regenerate the frontend client: `npm run generate:api`.
- The generated client is stored in `web/src/api/generated/` and must be committed.
- Never manually edit files inside `web/src/api/generated/` — they are always overwritten on regeneration.

---

## Commit and CI Rules

- Always run `npm install` after changing `web/package.json`.
- Always run `npm run generate:api` after changing any backend API contract or OpenAPI file.
- Run `./gradlew clean` if Gradle sync or compile fails.
- Never edit a committed Flyway migration file.
- All three backend modules must compile cleanly before a PR is merged.

---

## Pre-Commit Hook (Husky)

A Husky `pre-commit` hook is installed at `.husky/pre-commit` and enforces the following in order:

### Step 1 — Regenerate API client

```bash
npm run generate:api
```

Keeps the generated frontend client in sync with the backend contract before every commit.

### Step 2 — Secret scanning

```bash
git diff --cached | grep -E -q "password:\s*[A-Za-z0-9_\-]+"
```

Scans the staged diff for obvious plaintext password patterns.
If a match is found, the commit is aborted with a clear error message.

> This catches common accidental leaks only. Heavy secret scanning should run in CI, not pre-commit.

### Step 3 — Run tests

```bash
./gradlew test --continuous=false
```

If any test fails (non-zero exit code), the commit is aborted.

---

## Recommended CI Jobs

| Job | Command | Trigger |
|---|---|---|
| `backend-compile-wealth` | `./gradlew :application:finance:compileJava` | Every push |
| `backend-compile-health` | `./gradlew :application:health:compileJava` | Every push |
| `backend-compile-household` | `./gradlew :application:records:compileJava` | Every push |
| `backend-test` | `./gradlew test --continuous=false` | Every push |
| `frontend-build` | `cd web && npm install && npm run generate:api && npm run build` | Every push |
| `migration-check` | `./gradlew :application:finance:quarkusDev` (startup only) | Every push |
| `docs-check` | Verify all docs present, README tree updated | Every PR |

---

## Environment Variables Required in CI

| Variable | Used By | Description |
|---|---|---|
| `DB_USER` | Wealth, Household | PostgreSQL username |
| `DB_PASSWORD` | Wealth, Household | PostgreSQL password |
| `DB_URL` | Wealth, Household | PostgreSQL JDBC connection URL |
| `MONGO_URL` | Health | MongoDB connection string |

Set these as CI secrets (e.g., GitHub Actions Secrets) — never hardcode in source files.