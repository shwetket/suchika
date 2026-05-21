# GETTING_STARTED

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

This command regenerates the API client in `web/src/api/generated/`.

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

- Java 25
- Gradle wrapper (`./gradlew` / `gradlew.bat`)
- PostgreSQL

### Create database and user

```sql
CREATE DATABASE app_db;
CREATE USER app_user WITH PASSWORD 'yourpassword';
GRANT ALL PRIVILEGES ON DATABASE app_db TO app_user;
```

### Configure environment

Create `application/finance/.env`:

```properties
DB_USER=app_user
DB_PASSWORD=yourpassword
DB_URL=jdbc:postgresql://localhost:5432/app_db
```

### Run backend

```bash
./gradlew :application:finance:quarkusDev
```

Backend runs at `http://localhost:8080`.

### API docs

- Swagger UI: `http://localhost:8080/swagger-ui`
- OpenAPI JSON: `http://localhost:8080/q/openapi`

---

## Infrastructure Setup

### Database

- Single PostgreSQL database: `app_db`
- Both Finance and Health domains share the same database
- Migrations live in `application/finance/src/main/resources/db/migration/`

### Ports

- Backend: `8080`
- Frontend: `3000`

### Notes

- No Docker is required for local development.
- Ensure ports 8080 and 3000 are free.
- The backend reads DB credentials from `application/finance/.env`.

### Troubleshooting

| Issue | Solution |
|---|---|
| Backend cannot connect to DB | Verify `application/finance/.env` and PostgreSQL status |
| Port 8080 in use | Stop the conflicting process or change the port in `application.properties` |
| `npm install` fails | Run `npm install --legacy-peer-deps` in `web/` if needed |
| API client out of sync | Re-run `npm run generate:api` after backend or OpenAPI changes |
