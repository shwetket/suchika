# Contributing & Getting Started

This file covers local development setup, prerequisites, and run commands.

---

## Frontend Setup

### Prerequisites

- Node.js 18+
- npm
- Working backend service at `http://localhost:8080`
- OpenAPI contract available via the backend

### Install frontend dependencies

```bash
cd web
npm install
```

### Generate API client

```bash
cd web
npm run generate:api
```

This regenerates the typed API client in `web/src/api/generated/` from the live OpenAPI spec.
Run this every time the backend API contract changes.

### Run frontend

```bash
cd web
npm start
```

Frontend runs at `http://localhost:3000`.

### Build frontend

```bash
cd web
npm run build
```

---

## Backend Setup

### Prerequisites

- **Java 25** — this project targets Java 25 to stay aligned with the latest LTS release cycle and leverage modern language features (e.g., records, pattern matching, virtual threads via Loom). Ensure your local JDK is Java 25+.
- Gradle wrapper (`./gradlew` / `gradlew.bat`) — no separate Gradle install needed
- PostgreSQL — for Wealth and Household domains
- MongoDB — for Health domain

### PostgreSQL: Create database and user

```sql
CREATE DATABASE app_db;
CREATE USER app_user WITH PASSWORD 'yourpassword';
GRANT ALL PRIVILEGES ON DATABASE app_db TO app_user;
```

### MongoDB: Create database

```bash
# Start MongoDB locally (default port 27017)
mongod --dbpath /your/data/path

# MongoDB creates the database on first write — no manual creation needed
# Default connection: mongodb://localhost:27017/suchika_health
```

### Configure environment

Create `application/finance/.env`:

```properties
DB_USER=app_user
DB_PASSWORD=yourpassword
DB_URL=jdbc:postgresql://localhost:5432/app_db
MONGO_URL=mongodb://localhost:27017/suchika_health
```

### Run backend modules

Run all three modules in separate terminals:

```bash
# Wealth domain
./gradlew :application:finance:quarkusDev

# Health domain
./gradlew :application:health:quarkusDev

# Household domain
./gradlew :application:records:quarkusDev
```

All modules are served from the single Quarkus runtime at `http://localhost:8080`.

### API docs

- Swagger UI: `http://localhost:8080/swagger-ui`
- OpenAPI JSON: `http://localhost:8080/q/openapi`

---

## Pre-Commit Hook Setup

The project uses Husky to enforce checks before every commit.

### Install Husky

```bash
npm install
```

Husky is installed automatically via the `prepare` script in `package.json`.

### What the pre-commit hook does

1. Runs `npm run generate:api` — keeps the generated API client in sync.
2. Scans staged diff for plaintext password patterns — aborts commit if found.
3. Runs `./gradlew test` — aborts commit if any test fails.

### Manual hook trigger (for testing)

```bash
.husky/pre-commit
```

---

## Infrastructure Setup

### Ports

| Service | Port |
|---|---|
| Backend (Quarkus) | 8080 |
| Frontend (React) | 3000 |
| PostgreSQL | 5432 |
| MongoDB | 27017 |

### Notes

- No Docker required for local development.
- Ensure all four ports above are free before starting.
- The backend reads DB credentials from `application/finance/.env`.
- Flyway runs migrations automatically on backend startup — do not edit committed migration files.

---

## Troubleshooting

| Issue | Solution |
|---|---|
| Backend cannot connect to PostgreSQL | Verify `application/finance/.env` and PostgreSQL service status |
| Backend cannot connect to MongoDB | Verify MongoDB is running on port 27017 and `MONGO_URL` is set in `.env` |
| Port 8080 in use | Stop the conflicting process or change port in `application.properties` |
| `npm install` fails | Run `npm install --legacy-peer-deps` in `web/` |
| API client out of sync | Re-run `npm run generate:api` after any backend API change |
| Gradle compile fails | Run `./gradlew clean` then retry |
| Flyway migration error on startup | Never edit a previously run migration — create a new versioned file instead |
| Pre-commit hook not running | Run `npm install` to reinstall Husky |
| Backend fails to start | Check for any errors in the logs and ensure all dependencies are correctly installed and configured |