# Architecture Guidelines

> Best practices for all developers. Follow these on every PR.
> Enforced by ArchUnit in `shared/src/test/java/.../DomainRulesTest.java` — read that file before adding new classes.

---

## Domain Layer Rules

- `domain/` must have **zero** dependencies on frameworks, adapters, or other domains.
- No `@Inject`, no JPA annotations (`jakarta.persistence.*`), no HTTP types in `domain/`.
- Business rules live here. Nothing else does.
- ArchUnit enforces this automatically on every `./gradlew test` run.

---

## Hexagonal Layer Rules

Each domain has exactly three layers:

| Layer | Package | Rule |
|---|---|---|
| `domain/` | Pure Java entities + business logic | Zero framework deps |
| `ports/` | Use case interfaces (input) + repository interfaces (output) | Zero framework deps |
| `adapters/` | HTTP controllers + Panache/JPA persistence | Framework allowed here only |

- `adapters/` depends on `ports/` and `domain/` — never the reverse.
- Cross-domain logic goes through API calls between services — no shared DB joins.

---

## Multi-Service Architecture

Each domain runs as a separate Quarkus service on its own port. They share a single PostgreSQL database but each owns its own schema.

| Service | Port | Schema |
|---|---|---|
| Profile | 8081 | `profile` |
| Wealth | 8082 | `wealth` |
| Health | 8083 | `health` |
| Household | 8084 | `household` |
| Web Gateway (BFF) | 8080 | `projections` (read-only, CQRS snapshots) |

**Profile must start first** — all other services' Flyway migrations reference `profile.profile`.

The web-gateway has no database of its own. It aggregates domain REST calls and serves the React frontend.

---

## Database Rules

- **No cross-domain SQL joins. Ever.** Each domain service only queries its own schema.
- All schema changes go through versioned Flyway migrations in `application/flyway/{domain}/`.
- `application/flyway/00_bootstrap.sql` is run manually once as superuser — Flyway does not manage it.
- Never edit a committed migration file — create a new versioned file.
- No SQL ENUMs. Use plain `VARCHAR` for discriminator columns; enforce allowed values at the OpenAPI contract + Java enum level.

**DB constraint philosophy:**
- Keep in DB: `NOT NULL`, `PK`, `FK`, `UNIQUE`, business-rule `CHECK` constraints (`amount >= 0`, `end_date >= start_date`).
- Do NOT add to DB: enum discriminators (account types, event types, vital types). These change by adding a value to the OpenAPI enum + Java enum, with no Flyway migration needed.

---

## API Rules

- Frontend talks only to the Web Gateway (BFF) at `http://localhost:8080`.
- Domain services (8081–8084) are internal — the frontend never calls them directly.
- Use the generated OpenAPI client on the frontend — never hand-roll HTTP calls or edit `web/src/api/generated.ts` manually.
- Regenerate after any contract change: `cd web && npm run generate:api`.
- Contract files live in `application/contract/{domain}.yaml`.

---

## Frontend Rules

- Frontend lives in `web/`. No backend logic here.
- Tailwind CSS only — no CSS modules, no `style={{}}`, no other CSS frameworks.
- State and presentation are separate from business rules.
- Route paths are segmented by domain: `/wealth`, `/household`, `/health`.

---

## Logging and Exceptions

- Use `AppLogger` from `shared/` for all logging. No custom loggers per module.
- Throw typed exceptions from `shared/exception/` hierarchy (`NotFoundException`, `BadRequestException`, etc.).
- `ApplicationExceptionMapper` converts them to HTTP responses automatically.
- Never log passwords, tokens, or PII.

---

## Testing

- Domain logic is unit-tested with no framework setup (no Quarkus test harness needed).
- Adapter tests use the real DB where possible (Testcontainers preferred).
- No test should cross domain boundaries via the DB.
- ArchUnit tests in `shared/` enforce all the rules above automatically.
