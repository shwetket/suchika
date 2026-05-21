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
- **Source of truth for the frontend client is `application/contract/gateway.yaml`** — not the individual domain contracts.
- Domain contracts (`application/contract/{domain}.yaml`) serve only as the MicroProfile Rest Client spec inside `web-gateway`.

---

## Frontend Rules

- Frontend lives in `web/`. No backend logic here.
- Tailwind CSS only — no CSS modules, no `style={{}}`, no other CSS frameworks.
- State and presentation are separate from business rules.
- Route paths are segmented by domain: `/wealth`, `/household`, `/health`.

---

## Profile-ID Scoping

- Every DB query in every domain must filter by the active `profile_id`.
- This filter is injected in the `adapters/` layer only — never in `domain/` or `ports/`.
- Domain entities and use cases must never receive or hold tenant-isolation logic.
- ArchUnit rules in `shared/` enforce that the domain layer stays free of adapter concerns.

---

## Logging and Exceptions

- Use `AppLogger` from `shared/` for all logging. No custom loggers per module.
- Throw typed exceptions from `shared/exception/` hierarchy (`NotFoundException`, `BadRequestException`, etc.).
- `ApplicationExceptionMapper` converts them to HTTP responses automatically.
- Never log passwords, tokens, or PII.

---

## Testing

- Domain logic is unit-tested with no framework setup (no Quarkus test harness needed).
- Adapter tests use the real DB — Testcontainers with real PostgreSQL. No H2, no in-memory mocks.
- No test should cross domain boundaries via the DB.

**Web Gateway (BFF) Tests — `@InjectMock @RestClient` pattern (ADR-011):**
- Gateway tests use `@QuarkusTest` + RestAssured but do **not** require live downstream domain services.
- Inject mock Rest Clients with `@InjectMock @RestClient` to isolate the gateway from domain service availability.
- Each test stubs the downstream call (e.g., `Mockito.when(profileClient.getProfile(...)).thenReturn(...)`) and asserts only gateway behavior — routing, aggregation, response mapping.
- This replaces the earlier "live services + Flyway seeding" approach, which required all four domain services to be running during CI.

**Flyway Repeatable Migrations (domain adapter tests only):**
- `R__seed_*_test_data.sql` files under `application/flyway/test-seed/{domain}/` seed well-known records (Admin `00000000-0000-0000-0000-000000000001`, Profile `00000000-0000-0000-0000-000000000002`).
- These run automatically in `dev` and `test` profiles via `%test.quarkus.flyway.locations`.
- Domain adapter integration tests reference these seeded records or perform self-contained writes against them.
- ArchUnit tests in `shared/` enforce all layer rules automatically on every `./gradlew test` run.


