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

### Environment variable baseline and `.env.template`

- All runtime secrets and credentials are parameterized via environment variables.
  - Database URL: `DB_URL`
  - Database username: `DB_USERNAME`
  - Database password: `DB_PASSWORD`
- Each backend module reads environment variables with sensible fallbacks in `src/main/resources/application.properties`, e.g.:
  - `quarkus.datasource.username=${DB_USERNAME:app_user}`
  - `quarkus.datasource.password=${DB_PASSWORD:local_dev_only}`
  - `quarkus.datasource.jdbc.url=${DB_URL:jdbc:postgresql://localhost:5432/suchika_default}`
- Developers should not commit plaintext `.env` files. Use the repository template at `infrastructure/local/.env.template` to stage local values during development.
  - `infrastructure/local/.env.template` contains placeholders for `DB_URL`, `DB_USERNAME`, and `DB_PASSWORD`.
  - Local files such as `.env` or `infrastructure/local/.env` are excluded by `.gitignore`.

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

### Architecture enforcement (ArchUnit)

- Architecture tests live alongside unit tests and enforce package-level rules using ArchUnit.
- Canonical package layout enforced per service (example `finance` / `health`):
  - `com.suchika.<service>.domain` — core business objects and logic
  - `com.suchika.<service>.ports..` — port interfaces (`ports.in`, `ports.out`)
  - `com.suchika.<service>.application` — use-case orchestration and application services
  - `com.suchika.<service>.adapters..` — adapters (e.g., `adapters.in.http`, `adapters.out.persistence`)

- Exact ArchUnit rules enforced by tests (`HexagonalArchitectureTest.java`):
  1. Domain classes (`..domain..`) must NOT depend on any classes in `..ports..` or `..adapters..`.
  2. Port classes (`..ports..`) must NOT depend on any classes in `..adapters..`.
  3. Adapter classes (`..adapters..`) must NOT depend on classes in `..domain..` or `..application..` (adapters interact with domain only via ports).

- Why a build might fail:
  - If any class violates one of the rules above, the corresponding ArchUnit test will fail and cause the Gradle `test` task to return a non-zero exit status.
  - Example failure: an adapter directly referencing a domain entity or calling application-layer code will trigger a test failure.

- Notes about toolchain compatibility:
  - ArchUnit relies on ASM to import compiled class files. When using very new JDK class file versions (e.g., Java 25 / class file major version 69), older ArchUnit/ASM versions may be unable to parse classes and will throw an import error.
  - Current tests are written to skip (not fail) when the ArchUnit importer throws an error (this avoids blocking local development on unsupported toolchains). In CI we recommend running with a compatible JDK / ArchUnit version so rules execute reliably.
  - To make enforcement strict, upgrade ArchUnit/ASM to a version compatible with your JDK, or configure the test JVM target compatibility so that class files are importable by ArchUnit.
