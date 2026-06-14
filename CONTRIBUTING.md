# Contributing & Getting Started

This file covers local development setup, prerequisites, and run commands.

---

## Prerequisites

| Tool | Version | Purpose |
|---|---|---|
| Java | 25+ | Backend runtime |
| Gradle wrapper | (bundled) | No separate install needed |
| Node.js | 18+ | Frontend |
| PostgreSQL | 14+ | All domains (single instance) |

No MongoDB required. All domains use PostgreSQL via a schema-per-domain architecture.

---

## One-Time Database Setup

### 1. Create the database and run the bootstrap script

```bash
# Connect as superuser and create the database
psql -U postgres -c "CREATE DATABASE app_db;"

# Run the bootstrap (creates schemas, app_user, and grants)
psql -U postgres -d app_db -f application/flyway/00_bootstrap.sql
```

The bootstrap creates five schemas (`profile`, `wealth`, `household`, `health`, `projections`)
and a single `app_user` role used by all modules in local dev.

### 2. Configure environment

Copy the template and set your password:

```bash
cp infrastructure/local/.env.template application/finance/.env
```

Edit `application/finance/.env`:
```properties
DB_URL=jdbc:postgresql://localhost:5432/app_db
DB_USERNAME=app_user
DB_PASSWORD=your_secure_password_here
```

---

## Running the Backend

Each domain is an independent Quarkus module. Run in separate terminals.
Flyway migrations run automatically on startup — schemas are populated on first run.

```bash
# Profile domain (run first — other domains FK into profile)
./gradlew :application:domain:profile:adapters:quarkusDev

# Wealth domain
./gradlew :application:domain:wealth:adapters:quarkusDev

# Health domain
./gradlew :application:domain:health:adapters:quarkusDev

# Household domain
./gradlew :application:domain:household:adapters:quarkusDev
```

All modules are served from a single Quarkus runtime at `http://localhost:8080`.

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

Run `npm run generate:api` every time the backend API contract changes.

---

## Tests

```bash
# All backend tests
./gradlew test --continuous=false

# Single module tests
./gradlew :application:domain:wealth:domain:test

# Frontend tests
cd web && npm run test:ci
```

---

## Pre-Commit Hook

Husky runs automatically on every commit:
1. `npm run generate:api` — keeps the API client in sync
2. Scans staged diff for plaintext passwords
3. `./gradlew test` — aborts if any test fails

Install Husky (one-time, after `npm install` in the root):
```bash
npm install
```

---

## Migration Rules

Flyway migrations are in `application/flyway/{domain}/` and run automatically on module startup.

- **Never edit a committed migration file.** Create a new versioned file instead.
- `00_bootstrap.sql` is run manually once. Flyway does not manage it.
- Migration dependency order: **profile → wealth → household → health → projections**

---

## Ports

| Service | Port |
|---|---|
| Backend (Quarkus) | 8080 |
| Frontend (React) | 3000 |
| PostgreSQL | 5432 |
| SonarQube (local) | 9000 |

---

## Code Quality: SonarQube Community Edition (Local)

SonarQube provides comprehensive code quality analysis including architectural rules, security issues, and code smells.

### Setup (one-time)

1. **Download SonarQube Community Edition**
   - Go to: https://www.sonarsource.com/products/sonarqube/downloads/
   - Extract to a local folder

2. **Start SonarQube**
   ```bash
   cd /path/to/sonarqube/bin/windows-x86-64
   ./StartSonar.bat
   ```
   Then open `http://localhost:9000` (default login: `admin` / `admin`)

3. **Install Sonar Scanner**
   ```bash
   npm install -g sonar-scanner
   ```

4. **Create a project token**
   - Login to SonarQube
   - Click `Create new project` → set `Project key` to `suchika`
   - Go to `Security` → `Tokens` → generate and copy your token

5. **Update `sonar-project.properties`**
   ```bash
   # Replace YOUR_TOKEN with the token from step 4
   echo "sonar.login=YOUR_TOKEN" >> sonar-project.properties
   ```

### Run Analysis

After building the backend (`./gradlew build --no-daemon`):

```bash
sonar-scanner
```

View results: `http://localhost:9000/projects/suchika`

### Use the Pre-Commit Script

The unified build script runs everything including SonarQube:

```bash
# Bash
bash scripts/build-local.sh

# PowerShell
.\scripts\build-local.ps1
```

This runs:
- Backend tests + ArchUnit
- Frontend tests + lint + format
- Full Sonar analysis (includes SonarQube server check)

---

## Troubleshooting

| Issue | Solution |
|---|---|
| Backend cannot connect to PostgreSQL | Check `application/finance/.env` and PostgreSQL service |
| Flyway migration error on startup | Never edit a previous migration — create a new versioned file |
| `npm install` fails | Run `npm install --legacy-peer-deps` in `web/` |
| API client out of sync | Run `cd web && npm run generate:api` |
| Gradle compile fails | Run `./gradlew clean` then retry |
| Pre-commit hook not running | Run `npm install` to reinstall Husky |
| Profile module Flyway fails | Ensure `00_bootstrap.sql` was run first (creates schemas) |
