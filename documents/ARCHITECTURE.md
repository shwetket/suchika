# ARCHITECTURE

## Frontend Architecture

The frontend is a React application located in `web/`.

- Uses `react-scripts` and standard Create React App structure.
- API client code is generated from OpenAPI and stored in `web/src/api/generated/`.
- The frontend communicates with the backend over HTTP at `http://localhost:8080`.
- Frontend concerns are isolated from backend concerns.

### Frontend responsibilities

- Render UI and navigation
- Call backend APIs for CSV upload and transaction data
- Use generated OpenAPI client for typed requests
- Keep state and presentation separate from business rules

## Backend Architecture

The backend is a unified Quarkus application in `application/finance/`.

### Hexagonal architecture

The system follows Ports and Adapters:

- `domain/` contains core business entities and logic.
- `ports/in/` defines use case interfaces.
- `application/` implements business workflows.
- `ports/out/` defines repository and external service interfaces.
- `adapters/in/http/` exposes controllers and HTTP endpoints.
- `adapters/out/persistence/` implements database access.

### Domain separation

The backend supports two domains:

- `finance` — transaction upload and storage
- `health` — profile, doctor visit, and vital readings

Each domain is self-contained and does not import the other domain.

### Key rules

- `domain/` must not depend on framework or adapters.
- `application/` orchestrates domain use cases without direct DB or HTTP dependencies.
- `adapters/out/` depend on `infrastructure/` for shared plumbing.
- `infrastructure/` contains only wiring, database pools, and shared configuration.
- `shared/` contains cross-cutting utilities such as logging and error handling.

## Infrastructure Design

The infrastructure layer provides shared services and environment configuration.

### Database and migrations

- PostgreSQL is the single database.
- Flyway migrations live in `application/finance/src/main/resources/db/migration/`.
- Migration files are sequential and immutable once run.

### Shared modules

- `infrastructure/` contains shared persistence and configuration code.
- `shared/` contains reusable auth, logging, and error utilities.
- `openapi/` contains API contract definitions for Finance and Health.

### Build system

- Root Gradle project uses Kotlin DSL.
- Backend modules use the Gradle wrapper (`./gradlew`).
- Frontend lives in `web/` and uses npm scripts.

### API contract

- Backend implements OpenAPI contracts from `openapi/`.
- Frontend uses generated client code from `web/src/api/generated/`.
- This keeps frontend and backend contracts aligned.
