# Architecture Decisions

| | |
|---|---|
| **Type** | Reference — ADR Log |
| **Audience** | All developers |
| **Status** | Active |
| **Last updated** | 2026-06-23 |

## Objective

Record every significant architectural decision made for this project, along with its rationale and current status. These decisions are final unless superseded by a new ADR — do not change without team review.

## Use Cases

- When questioning why a pattern is used ("why no SQL ENUMs?", "why mock the Rest Client in gateway tests?")
- Before proposing a change that touches a structural decision
- During onboarding to understand the design choices

## ADR Index

| ADR | Title | Status |
|---|---|---|
| [ADR-001](#adr-001-hexagonal-architecture-ports-and-adapters) | Hexagonal Architecture (Ports and Adapters) | Accepted |
| [ADR-002](#adr-002-five-separate-quarkus-services-four-domains--bff) | Five Separate Quarkus Services | Accepted |
| [ADR-003](#adr-003-no-cross-domain-db-joins) | No Cross-Domain DB Joins | Accepted |
| [ADR-004](#adr-004-single-postgresql-database-schema-per-domain) | Single PostgreSQL Database, Schema-Per-Domain | Accepted |
| [ADR-005](#adr-005-external-oidcauth2-for-identity-future) | External OIDC/OAuth2 for Identity | Accepted — deferred to v1.0 |
| [ADR-006](#adr-006-profile-scoped-data-isolation) | Profile-Scoped Data Isolation | Accepted |
| [ADR-007](#adr-007-application-layer-encryption-for-sensitive-wealth-data-future) | Application-Layer Encryption for Wealth Data | Accepted — deferred to v1.0 |
| [ADR-008](#adr-008-no-stored-refresh-tokens) | No Stored Refresh Tokens | Accepted |
| [ADR-009](#adr-009-openapi-contract-driven-frontend) | OpenAPI Contract-Driven Frontend | Accepted |
| [ADR-010](#adr-010-no-sql-enums--varchar-with-contract-level-validation) | No SQL ENUMs — VARCHAR + Contract Validation | Accepted |
| [ADR-011](#adr-011-gateway-test-isolation-via-injectmock-restclient) | Gateway Test Isolation via `@InjectMock @RestClient` | Accepted — v0.2 |
| [ADR-012](#adr-012-household-domain-deferred-to-v03) | Household Domain Deferred to v0.3 | Accepted — 2026-06-19 |
| [ADR-013](#adr-013-projection-calculation-engine-in-web-gateway) | Projection Calculation Engine in web-gateway | Accepted — 2026-06-24 |

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

**Decision:** Every DB query across all domains must be scoped to the active `profile_id`. Adapters inject this filter — never the domain or ports layer.

**Implementation:**
- All Panache repository methods in `adapters/out/persistence/` accept `profileId` as an explicit parameter and append it to every query predicate.
- Domain use case interfaces in `ports/in/` carry `profileId` as a method argument so adapters can pass it through — but domain entities never store or reason about it.
- `profile_id UUID REFERENCES profile.profile(id)` is a column on every domain table; the FK is enforced in the DB, the filter is enforced in the adapter.

**Rationale:** Ensures data isolation between household members without leaking multi-tenancy logic into business rules. Domain tests use plain `new` and never need a `profileId` context object.

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

---

## ADR-011: Gateway Test Isolation via `@InjectMock @RestClient`

**Status:** Accepted — implemented in v0.2

**Decision:** Web-gateway (`web-gateway`) integration tests use `@QuarkusTest` + RestAssured with `@InjectMock @RestClient` to stub downstream domain service calls. Live domain services are not required during gateway test runs.

**Pattern:**
```java
@QuarkusTest
class ProfileGatewayResourceTest {
    @InjectMock
    @RestClient
    ProfileServiceClient profileClient;

    @Test
    void getProfile_returns200() {
        Mockito.when(profileClient.getProfile(any())).thenReturn(stubbedResponse());
        given().when().get("/api/v1/gateway/profiles/{id}", PROFILE_ID)
               .then().statusCode(200);
    }
}
```

**Rationale:** The earlier approach required all four domain services to be running (with Flyway-seeded data) before gateway tests could execute. This made CI fragile and slow. With `@InjectMock @RestClient`, gateway tests verify only gateway logic — routing, aggregation, response shaping — independent of service availability. Domain adapter tests continue to use Testcontainers for real DB coverage.

---

## ADR-012: Household Domain Deferred to v0.3

**Status:** Accepted — decided 2026-06-19

**Decision:** The `household` domain (calendar events, inventory items, goals) is not implemented in v0.2. The Household Quarkus service module skeleton exists at `:application:domain:household:adapters` (port 8084) but contains zero Java files. `HouseholdGatewayResource` in `web-gateway` is not implemented. Frontend pages under `src/pages/Household/` show "Coming Soon" stubs.

**v0.3 work required:**
- Domain entities: `CalendarEvent`, `InventoryItem`, `Goal`
- Ports, services, JPA persistence, JAX-RS controllers
- Flyway migrations under `application/flyway/household/`
- `HouseholdServiceClient` + `HouseholdGatewayResource`
- Household paths added to `application/contract/gateway.yaml`
- Frontend: `Calendar.js`, `Inventory.js`, `Goals.js` pages + Jest tests

**Rationale:** Household has zero backend files — building it before UAT would delay the pilot window significantly. Profile + Wealth + Health cover all high-priority user actions for the v0.2 pilot. Household feedback can be collected separately in v0.3.

---

## ADR-013: Projection Calculation Engine in web-gateway

**Status:** Accepted — decided 2026-06-24

**Decision:** All cross-domain metric computations (net worth, goal progress, vitals summary, event counts) are owned by a single `ProjectionCalculationEngine` service class inside `web-gateway`. Results are persisted to `projections.dashboard_snapshot` via UPSERT. The dashboard read endpoint queries only the snapshot table — no computation at read time.

**Snapshot keys:**

| Key | Formula | Source |
|---|---|---|
| `WEALTH_NET_WORTH` | Sum of balances across all active accounts | WealthServiceClient |
| `WEALTH_GOAL_PROGRESS` | Per-goal: target, current (derived from wealth txns), % | WealthServiceClient + HouseholdServiceClient |
| `HEALTH_VITALS_SUMMARY` | Latest reading per vital type | HealthServiceClient |
| `HOUSEHOLD_EVENT_SUMMARY` | Upcoming events count (next 30 days) | HouseholdServiceClient |

**Trigger:** On-demand via `POST /v1/projections/refresh/{profileId}`. The endpoint is synchronous — it returns 200 when all snapshots are written. The frontend shows a non-blocking progress indicator; the user can navigate away while the refresh runs.

**Extension pattern:** Adding a new metric = one new method in `ProjectionCalculationEngine` + one new snapshot key constant. No other changes needed.

**Goal `current_amount` write-back:** After computing goal progress from wealth transactions, the engine calls `PUT /v1/goals/{id}/current-amount` on the household service to persist the computed value. This is an internal-only endpoint — not exposed through the gateway contract for direct client use.

**Rationale:** Separates the compute path from the read path. Dashboard reads are instant (single DB `SELECT`). Math is isolated and independently testable. New formulas require no changes to the dashboard endpoint. Follows the CQRS projection pattern already established by `projections.dashboard_snapshot`.
