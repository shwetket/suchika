# Architecture Guidelines

> Best practices for all developers. Follow these on every PR.

---

## Domain Layer Rules

- `domain/` must have **zero** dependencies on frameworks, adapters, or other domains.
- No `@Inject`, no JPA annotations, no HTTP types in `domain/`.
- Business rules live here. Nothing else does.

---

## Application Layer Rules

- `application/` orchestrates use cases. It calls ports — never adapters or DB directly.
- No `HttpServletRequest`, no SQL, no direct MongoDB calls here.
- Cross-domain logic goes through `shared/` orchestration interfaces only.

---

## Adapter Rules

- `adapters/out/` depend on `infrastructure/` for shared plumbing (DB pools, config).
- `adapters/in/http/` are thin. Translate HTTP → domain input. No business logic.
- Adapters inject the active `profile_id` into every query. Domain layer never does this.

---

## Database Rules

- **No cross-domain SQL joins. Ever.**
- All PostgreSQL schema changes use versioned Flyway migrations. No manual edits on persistent DBs.
- MongoDB collections are schema-validated at the application layer, not DB layer.
- Each domain owns its tables. Other domains never read them directly.

---

## Security Rules

- Every endpoint validates the active profile's role before processing.
- Restricted profiles (Children) must not trigger queries to unauthorized domains (e.g., no Wealth queries from a Child profile).
- Only short-lived OAuth access tokens for external integrations. No refresh tokens in DB.
- Encrypt sensitive financial data at `adapters.out.persistence` before insertion.
- Export data must be encrypted and signed. Import requires signature verification.

---

## API Rules

- All endpoints served from the single Quarkus runtime on port `8080`.
- Use generated OpenAPI clients on the frontend — never hand-roll HTTP calls.
- Regenerate client after any spec change: `npm run generate:api`.
- Cross-domain composite data goes through dedicated endpoints (`/api/v1/dashboard/actions`, `/api/v1/trips/{event_id}/feasibility`) — not by calling multiple domain APIs from the frontend.

---

## Frontend Rules

- Frontend lives in `web/`. No backend logic here.
- API client code is generated — do not edit files in `web/src/api/generated/` by hand.
- Route paths are segmented by domain: `/wealth`, `/household`, `/health`.
- State and presentation are separate from business rules.

---

## Logging & Audit

- All data access and modifications must be logged with timestamps and user IDs.
- Use the shared logging utility in `shared/` — do not roll custom loggers per module.

---

## Testing

- Domain logic is unit-tested with no framework setup required (no Spring context, no Quarkus test harness).
- Adapter tests use the real DB where possible (Testcontainers preferred).
- No test should cross domain boundaries via the DB.
