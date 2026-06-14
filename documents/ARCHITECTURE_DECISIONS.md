# Architecture Decisions

> These decisions are final. They define the core structure of this repository.
> Do not change without team review and a new ADR entry.

---

## ADR-001: Hexagonal Architecture (Ports and Adapters)

**Status:** Accepted

**Decision:** All domain modules follow Ports and Adapters.

**Structure:**
- `domain/` — core entities and logic, zero framework deps
- `ports/in/` — input use case interfaces
- `ports/out/` — output repository/service interfaces
- `adapters/` — HTTP controllers (in) + Panache/JPA persistence (out)

**Rationale:** Keeps business logic testable and framework-independent. Swapping DB or HTTP layer does not touch domain code. Enforced by ArchUnit tests in `shared/`.

---

## ADR-002: Five Separate Quarkus Services (Four Domains + BFF)

**Status:** Accepted

**Decision:** Each domain runs as its own Quarkus service with a dedicated HTTP port. A fifth service, `web-gateway`, acts as a BFF (Backend for Frontend) aggregating domain REST calls for the React frontend.

| Service | Gradle module | Port | Schema |
|---|---|---|---|
| Profile | `:application:domain:profile:adapters` | 8081 | `profile` |
| Wealth | `:application:domain:wealth:adapters` | 8082 | `wealth` |
| Health | `:application:domain:health:adapters` | 8083 | `health` |
| Household | `:application:domain:household:adapters` | 8084 | `household` |
| Web Gateway (BFF) | `:application:web-gateway` | 8080 | `projections` (read-only) |

All five services share one PostgreSQL database (`app_db`), each owning a separate schema. The web-gateway has no DB dependency — it composes domain REST calls and runs CQRS read projections.

**Rationale:** Domain isolation enforced at both code and process boundaries. No port conflicts when running all services simultaneously. The BFF shields the frontend from internal service topology.

---

## ADR-003: No Cross-Domain DB Joins

**Status:** Accepted

**Decision:** No SQL joins across domain schemas. Ever. Cross-domain data flows through REST calls between services or through the `web-gateway` BFF.

**Rationale:** Preserves domain isolation. Each service can evolve its schema independently. Prevents tight coupling that makes independent scaling impossible.

---

## ADR-004: Single PostgreSQL Database, Schema-Per-Domain

**Status:** Accepted

**Decision:** All domains use a single PostgreSQL database (`app_db`) with five schemas: `profile`, `wealth`, `health`, `household`, `projections`. All schema changes go through Flyway migrations in `application/flyway/{domain}/`.

| Schema | Owner | Key tables |
|---|---|---|
| `profile` | Profile service | `admin`, `profile` |
| `wealth` | Wealth service | `account`, `transaction`, `statement_upload`, `physical_asset` |
| `health` | Health service | `vital_reading`, `doctor_visit` |
| `household` | Household service | `calendar_event`, `inventory_item`, `goal` |
| `projections` | Web Gateway | `dashboard_snapshot` (CQRS read model) |

**Rationale:** Health domain data (vitals, doctor visits) is relational and benefits from schema enforcement and FK constraints back to `profile.profile`. A single PostgreSQL instance is simpler to operate for a personal household system at current scale.

---

## ADR-005: External OIDC/OAuth2 for Identity (Future)

**Status:** Accepted — deferred to v1.0

**Decision:** Identity management will be fully delegated to an external OIDC/OAuth2 provider. Quarkus manages the session context. Until v1.0, auth is not implemented.

**Rationale:** Avoids rolling custom auth. Standardizes token handling.

---

## ADR-006: Profile-Scoped Data Isolation

**Status:** Accepted

**Decision:** Every DB query across all domains must be scoped to the active `profile_id`. Adapters inject this filter — never the domain layer.

**Rationale:** Ensures multi-tenancy safety without leaking tenant logic into business rules.

---

## ADR-007: Application-Layer Encryption for Sensitive Wealth Data (Future)

**Status:** Accepted — deferred to v1.0

**Decision:** Sensitive financial ledgers in the `wealth` domain will be encrypted at `adapters/out/persistence/` before DB insertion.

**Rationale:** Defense-in-depth. DB-level compromise does not expose plaintext financial data.

---

## ADR-008: No Stored Refresh Tokens

**Status:** Accepted

**Decision:** Only short-lived OAuth access tokens are used for external integrations. Storing offline or refresh tokens in the database is strictly prohibited.

**Rationale:** Limits blast radius of a DB breach on third-party integrations.

---

## ADR-009: OpenAPI Contract-Driven Frontend

**Status:** Accepted

**Decision:** The frontend uses a generated API client from the gateway OpenAPI spec. Generated code lives in `web/src/api/generated.ts`. The contract file is `application/contract/gateway.yaml`. Never hand-edit the generated file.

Domain service contracts live in `application/contract/{domain}.yaml` and are mirrored in `application/web-gateway/src/main/resources/{domain}.yaml` for MicroProfile Rest Client use.

Regenerate after any contract change:
```bash
cd web && npm run generate:api
```

**Rationale:** Typed, contract-driven requests catch integration bugs at generation time, not runtime.

---

## ADR-010: No SQL ENUMs — VARCHAR with Contract-Level Validation

**Status:** Accepted

**Decision:** Discriminator columns (account types, event types, vital types, relation values, etc.) use plain `VARCHAR` with no `CHECK` constraint in PostgreSQL. Allowed values are enforced at the OpenAPI contract (enum on the schema field) and Java enum + `@Valid` annotation.

**Rationale:** Adding a new discriminator value requires only a contract + code change — no Flyway migration needed. SQL ENUMs require `ALTER TYPE` to add values, creating unnecessary migration friction.
