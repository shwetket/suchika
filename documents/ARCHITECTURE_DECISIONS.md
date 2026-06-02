# Architecture Decisions

> These decisions are final. They define the core structure of this repository.
> Do not change without team review and a new ADR entry.

---

## ADR-001: Hexagonal Architecture (Ports and Adapters)

**Status:** Accepted

**Decision:** All modules follow Ports and Adapters.

**Structure:**
- `domain/` — core entities and logic, zero framework deps
- `ports/in/` — input use case interfaces
- `ports/out/` — output repository/service interfaces
- `application/` — orchestrates use cases
- `adapters/in/http/` — REST controllers
- `adapters/out/persistence/` — DB implementations

**Rationale:** Keeps business logic testable and framework-independent. Swapping DB or HTTP layer does not touch domain code.

---

## ADR-002: Single Quarkus Runtime, Three Domain Modules

**Status:** Accepted

**Decision:** One Quarkus app on port `8080` serving all three domains (`wealth`, `household`, `health`).

| Module | Domain | DB |
|---|---|---|
| `application/wealth` | Transactions, accounts, vehicles | PostgreSQL |
| `application/health` | Biometric tracking | MongoDB |
| `application/household` | Profiles, calendar, inventory, automation | PostgreSQL |

**Rationale:** Avoids microservice overhead at current scale. Domains are isolated by code boundaries, not process boundaries.

---

## ADR-003: No Cross-Domain DB Joins

**Status:** Accepted

**Decision:** No SQL joins across domain tables. Ever. Cross-domain data flows through API boundaries or `shared/` orchestration only.

**Rationale:** Preserves domain isolation. Prevents tight DB coupling that makes independent evolution impossible.

---

## ADR-004: PostgreSQL (Flyway) for Wealth & Household, MongoDB for Health

**Status:** Accepted

**Decision:**
- Wealth and Household use PostgreSQL with versioned Flyway migrations.
- Health uses MongoDB (schema-less, validated at the application layer).

**Rationale:** Health data is unstructured time-series biometrics — document model fits better. Financial and household data is relational and benefits from schema enforcement.

**Rule:** No manual schema edits on persistent databases. All PostgreSQL changes go through Flyway migrations.

---

## ADR-005: External OIDC/OAuth2 for Identity

**Status:** Accepted

**Decision:** Identity management is fully delegated to an external OIDC/OAuth2 provider. Quarkus manages the session context.

**Rationale:** Avoids rolling custom auth. Standardizes token handling.

---

## ADR-006: Profile-Scoped Data Isolation

**Status:** Accepted

**Decision:** Every DB query across all domains must be scoped to the active `profile_id`. Adapters inject this filter — never the domain layer.

**Rationale:** Ensures multi-tenancy safety without leaking tenant logic into business rules.

---

## ADR-007: Application-Layer Encryption for Sensitive Wealth Data

**Status:** Accepted

**Decision:** Sensitive financial ledgers in the `wealth` domain are encrypted at `adapters.out.persistence` before DB insertion.

**Rationale:** Defense-in-depth. DB-level compromise does not expose plaintext financial data.

---

## ADR-008: No Stored Refresh Tokens

**Status:** Accepted

**Decision:** Only short-lived OAuth access tokens are used for external integrations. Storing offline or refresh tokens in the database is strictly prohibited.

**Rationale:** Limits blast radius of a DB breach on third-party integrations.

---

## ADR-009: OpenAPI Contract-Driven Frontend

**Status:** Accepted

**Decision:** Frontend uses generated API clients from OpenAPI specs (`npm run generate:api`). Generated code lives in `web/src/api/generated/`. Specs live in `openapi/wealth.yaml`, `openapi/health.yaml`, `openapi/household.yaml`.

**Rationale:** Typed, contract-driven requests catch integration bugs at generation time, not runtime.
