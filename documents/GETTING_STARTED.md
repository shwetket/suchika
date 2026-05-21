# Getting Started — Local Development Setup

## Prerequisites

| Tool | Version |
|---|---|
| Java | 25.0.x |
| Gradle | 9.3.0 (via wrapper — no install needed) |
| PostgreSQL | Any recent version, running locally |
| Node.js | 18+ |
| IntelliJ IDEA | Any recent version (recommended) |

---

## Step 1 — Clone and open in IDE

```bash
git clone REPO_URL
cd suchika
```

Open in IntelliJ IDEA:
- **File** → **Open** → select the root folder
- Gradle auto-syncs all modules

---

## Step 2 — Create PostgreSQL database and user

Using pgAdmin or psql CLI:

```sql
CREATE DATABASE app_db;
CREATE USER app_user WITH PASSWORD 'yourpassword';
GRANT ALL PRIVILEGES ON DATABASE app_db TO app_user;
```

---

## Step 3 — Create `.env` files

Create `application/finance/.env`:
```properties
DB_USER=app_user
DB_PASSWORD=yourpassword
DB_URL=jdbc:postgresql://localhost:5432/app_db
```

---

## Step 4 — Run the unified backend service

```bash
./gradlew :application:finance:quarkusDev
```

✅ Flyway runs automatically on first start and creates all tables in `app_db`.

Backend API runs at: `http://localhost:8080`

---

## Step 5 — Run React frontend (in another terminal)

```bash
cd web
npm install
npm start
```

Frontend runs at: `http://localhost:3000`

---

## Step 6 — View API documentation

Once the unified backend is running, open:

```
http://localhost:8080/swagger-ui
```

The OpenAPI spec is available at:

```
http://localhost:8080/q/openapi
```

---

## Generating the React API client

The frontend API client is auto-generated from OpenAPI contracts. Re-run whenever a contract changes:

```bash
cd web
npm run generate:api
```

Generated client is written to `web/src/api/generated/`.

---

## Known Setup Notes

- **JVM args:** Java 25 requires `--add-opens java.base/java.lang=ALL-UNNAMED` for Quarkus internals. This is configured in `build.gradle.kts` under `quarkusDev { jvmArgs }`.
- **Single database:** Both Finance and Health domains share `app_db`. Flyway migration versions are globally sequential (V1–V6) to avoid conflicts.
- **No Docker required:** PostgreSQL runs locally. No containerization needed for local development.
- **Port binding:** Make sure ports 8080 and 3000 are available.

---

## Troubleshooting

| Issue | Solution |
|---|---|
| `Connection refused` when running services | Ensure PostgreSQL is running and `DB_URL` in `.env` is correct |
| Flyway migration failed | Check that `app_db` database exists and user has permissions |
| `Port already in use` | Change port in `application.properties` or kill the process using the port |
| Gradle sync fails | Run `./gradlew clean` and re-open the project in IntelliJ |
