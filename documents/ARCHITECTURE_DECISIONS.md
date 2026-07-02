# Architecture Decisions

| | |
|---|---|
| **Type** | Reference — ADR Log |
| **Audience** | All developers |
| **Status** | Active |
| **Last updated** | 2026-07-02 (ADR-018 added) |

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
| [ADR-014](#adr-014-typed-parse-exception-extending-applicationexception) | Typed Parse Exception Extending ApplicationException | Accepted — 2026-06-29 |
| [ADR-015](#adr-015-uploadresult-as-a-ports-layer-return-type) | UploadResult as a Ports-Layer Return Type | Accepted — 2026-06-29 |
| [ADR-016](#adr-016-joint-account-ownership-via-designated-profile_id--metadata-attribution) | Joint Account Ownership via Designated `profile_id` + Metadata Attribution | Accepted — 2026-06-30 |
| [ADR-017](#adr-017-household-level-dashboard-aggregation) | Household-Level Dashboard Aggregation | Accepted — 2026-06-30 |
| [ADR-018](#adr-018-react-query-for-frontend-server-state) | React Query for Frontend Server State | Accepted — 2026-07-02 |

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

---

## ADR-014: Typed Parse Exception Extending ApplicationException

**Status:** Accepted — decided 2026-06-29

**Decision:** Domain-adjacent parsing errors (e.g., `CsvParseException` in the wealth adapters layer) extend `ApplicationException` from `shared/exception/` rather than any checked Java exception or bare `RuntimeException`. They carry structured fields (`errorType`, `missingColumns`) so that the error logger and HTTP mapper can handle them without string parsing.

**Implementation:**
- `CsvParseException` in `com.suchika.wealth.adapters.services` extends `ApplicationException`
- Factory methods (`missingDateColumn`, `missingAmountColumn`, `missingRequiredColumn`) produce typed instances with a fixed HTTP status (400) and error code (`BAD_REQUEST`)
- `StatementUploadService` catches `CsvParseException` explicitly before the generic `Exception` catch, persists structured fields to `upload_error_log`, then rethrows the exception so `ApplicationExceptionMapper` converts it to an HTTP 400 response

**Rationale:** Keeps error structure consistent with the rest of the `shared/` exception hierarchy. The adapter service can differentiate parse failures from system failures without inspecting message strings. The error log gets machine-readable `error_type` values, not free-text.

---

## ADR-015: UploadResult as a Ports-Layer Return Type

**Status:** Accepted — decided 2026-06-29

**Decision:** `UploadResult` lives in `com.suchika.wealth.ports.input` — the ports layer. It is not a domain entity and is not an adapter DTO. It is a structured return type for the `StatementUploadUseCase.uploadStatement()` port method, wrapping the persisted `StatementUpload` entity with per-row outcome metadata that does not belong on the domain entity itself.

**Structure:**
```java
// ports/input/UploadResult.java
public class UploadResult {
    StatementUpload upload;      // persisted domain entity
    int insertedCount;
    List<SkippedRow> skippedDuplicates;

    public record SkippedRow(LocalDate txnDate, BigDecimal amount, String description) {}
}
```

**Rationale:** `insertedCount` and `SkippedRow` are outcome metadata for the upload operation — they have no independent lifecycle and are not stored as domain state. Attaching them to `StatementUpload` would pollute the domain entity with operation-scoped data. Putting them in the adapter DTO layer would force the adapter to compute these values, breaking the single-responsibility of the service. The ports layer is the correct home: it defines the use case contract, and this result type is part of that contract.

---

## ADR-016: Joint Account Ownership via Designated `profile_id` + Metadata Attribution

**Status:** Accepted — decided 2026-06-30

**Decision:** A jointly-owned financial account (e.g., a Kotak Mahindra household expense account owned by two household members) is stored as a single `wealth.account` row with exactly one `profile_id` of record (the household admin's designated financial profile). The other co-owner(s) are recorded in `account.metadata.joint_owners: [profile_id, ...]` for display/attribution purposes only. This array is never used as a query predicate.

**Context:** `wealth.account` and ADR-006 assume one `profile_id` owns each account. A genuinely joint account breaks this assumption. Three options were considered: (1) single designated owner + metadata attribution array, (2) a many-to-many `account_owner` join table, (3) a new household-level ownership tier above `profile_id`.

**Rejected alternatives:**
- **Many-to-many `account_owner` table:** enables true multi-profile querying, but every wealth repository method, the `TransactionRepository` port interface, and the `projections.dashboard_snapshot` per-`(profile_id, snapshot_key)` model would need to change to support multi-owner queries. Also risks double-counting the same account's balance into two profiles' net worth simultaneously, which directly violates Epic 8's "zero leakage — counted exactly once" rule (`REQUIREMENTS_wealth_domain.md` Use Case 8.1).
- **Household-level ownership tier:** no `household` ownership concept exists today at the wealth or profile domain level. Introducing one is a much larger structural change to solve a problem affecting one account. Deferred until a real multi-tenant household need exists (YAGNI).

**Rationale:** Option 1 requires zero changes to `domain/`, `ports/`, the `TransactionRepository` interface, or the projection engine's snapshot key shape — `profile_id` stays a single scalar everywhere it already is. This keeps `profile_id` filtering adapter-only (ADR-006) and avoids any cross-domain or cross-profile query pattern. It follows the existing precedent set by `wealth.physical_asset.profile_id`, which is already nullable with the documented convention "NULL = owned by the admin profile" — same shape of problem (one row, ambiguous singular ownership), same style of resolution (pick a canonical owner, carry nuance in metadata).

**Schema impact:** Requires `wealth.account.metadata JSONB` (does not exist as of v0.4 — only `transaction` and `physical_asset` have a `metadata` column today). Added via Flyway `V6__account_metadata.sql` as part of Epic 8 Phase 1 — see `documents/EPIC8_IMPLEMENTATION_PLAN.md`.

**Open product question:** whether joint-account transactions should count toward both owners' *individual* net worth figures or only the designated owner's/household total — this is a financial-modeling policy choice, not a schema question. Tracked as `OpenQuestions.md` Q21.

**Update — 2026-06-30:** Q21 resolved. Superseded by ADR-017 — the dashboard's primary view is a household rollup, so individual-vs-joint attribution is now moot at the net-worth level; the Kotak account simply contributes to the one family total like every other account.

---

## ADR-017: Household-Level Dashboard Aggregation

**Status:** Accepted — decided 2026-06-30

**Decision:** All Epic 8 dashboard outputs (net worth, category subtotals, goals, EMI/loan tracking, validation results) are computed and stored as a **household-level rollup**, not per-individual-profile figures. The rollup aggregates across every `profile.profile` row sharing the admin's `admin_id`, and is the dashboard's default/primary view. Per-member sub-breakdowns (e.g., "Gayan's SIP portfolio: ₹6,000") are preserved as nested structure inside the rollup's JSON payload — never computed or stored as a separate snapshot row, and never gated behind separate authentication. Only the admin (Ketan) logs in; individual member profiles are data-attribution targets within his one session, not independent dashboard users.

**Context:** `ProjectionCalculationEngine` (ADR-013) currently computes one snapshot per `profile_id` per metric, keyed `(profile_id, snapshot_key)` in `projections.dashboard_snapshot`. The product owner clarified (resolving Q21) that he manages all family finances as head of household — his own `Financial_Data.md` is titled "Family Financial Data — Combined," with one net worth and one goal set, not per-person figures. He also clarified a second time (refining Q25): he is the *only* person who ever logs into the app; Shweta/Gayan/Vamika never have independent sessions. He wants both the family total (headline) and the ability to drill into any one member's data, from his single session — not two separate access levels.

**Does this violate ADR-006?** No — confirmed explicitly, not assumed:

ADR-006 says: "Every DB query across all domains must be scoped to the active `profile_id`. Adapters inject this filter — never the domain or ports layer." This rule governs the **domain adapter layer's SQL query pattern** inside each of the four domain services (profile/wealth/health/household) — it is about how `WHERE profile_id = ?` gets attached to a Panache query.

`ProjectionCalculationEngine` lives in `web-gateway`, which:
- Has no database of its own and issues zero SQL against any domain schema (ADR-002, ADR-013).
- Talks to each domain exclusively over REST, one `profile_id` per call — exactly the access pattern ADR-006 already permits and assumes (`WealthServiceClient.listAccounts(..., profileId)`, `ProfileServiceClient.listProfiles(adminId, isActive)`, etc.).

Looping the engine's existing per-profile compute calls across every member profile under one `admin_id` is **N sequential single-profile-scoped REST calls**, each individually ADR-006-compliant on the domain side. The aggregation (summing the N results into one family total) happens in gateway application memory, after each domain call has already returned ADR-006-filtered data. No query anywhere — gateway or domain — is ever scoped to more than one `profile_id` at a time. This is also not a cross-domain SQL join (ADR-003) for the same reason: nothing joins across schemas; the gateway composes REST responses.

**Conclusion: no amendment to ADR-006 needed.** The rollup is a gateway-layer composition concern, a layer ADR-006 does not govern. This is consistent with ADR-013's own framing: the engine "has read access to all domain schemas" via REST, never SQL.

**Mechanism — how the rollup is computed:**

1. Resolve household membership: `ProfileServiceClient.listProfiles(adminId, isActive=true)` — already exists today, no new gateway client code needed. Returns every `profile.profile` row where `admin_id` matches (the FK already established by `V2__add_admin_table.sql`).
2. For each member profile_id returned, call the existing per-profile compute path exactly as today (e.g., `wealthServiceClient.listAccounts(..., profileId)`).
3. Sum/aggregate the per-member results into one family total; retain each member's individual result as a nested entry.
4. UPSERT one row into `projections.dashboard_snapshot`, keyed by **the admin's own SELF profile_id** (the `profile.profile` row where `relation_to_admin = 'SELF'` and `admin_id` = the logged-in admin — this is already "Ketan's profile_id," the same identifier every existing single-profile snapshot uses today) and a new family-scoped `snapshot_key`.

**Why key by the admin's SELF profile_id, not `admin.id`:** `dashboard_snapshot.profile_id` is already typed/used as a `profile.profile.id` reference everywhere in the existing schema and code (`DashboardSnapshotRepository`, `ProjectionResource`). Introducing `admin.id` as a second identifier space into the same column would require a new column or a type-widening migration for zero benefit — the admin's SELF profile_id already uniquely and stably identifies "this household" today (`uq_admin_self_profile` guarantees exactly one SELF per admin). Reuse it.

**New snapshot keys** (additive — old keys are not deleted, see coexistence below):

| Key | Computed from |
|---|---|
| `WEALTH_NET_WORTH_FAMILY` | Sum of `computeNetWorth()`-equivalent across all members under admin_id |
| `WEALTH_GOAL_PROGRESS_FAMILY` | Family-level goal set (5 formula-driven goals, Epic 8 Phase 4) — goals are household-scoped by definition, no per-member variant exists |
| `WEALTH_VALIDATION_REPORT_FAMILY` | Phase 4 validation engine, run once across the combined ledger |
| `WEALTH_EMI_TRACKING_FAMILY` | Phase 3 loan/EMI aggregation across all member-owned loan accounts |

`HEALTH_VITALS_SUMMARY` and `HOUSEHOLD_EVENT_SUMMARY` are explicitly **not** rolled up under this ADR — vitals and calendar events are inherently per-person (Gayan's vitals are never summed with Vamika's), so they keep their existing per-profile semantics. This ADR is scoped to Epic 8's wealth outputs only, per the product owner's stated framing ("Family Financial Data — Combined").

**Payload shape — family total with nested per-member breakdown** (matches the product owner's reference `assets_06062026.json` shape — per-person figures nested under the family total, not flattened away):

```json
{
  "family_total": 4250000.00,
  "account_count": 9,
  "members": [
    { "profile_id": "<ketan-uuid>",  "name": "Ketan",  "subtotal": 1800000.00, "account_count": 4 },
    { "profile_id": "<shweta-uuid>", "name": "Shweta", "subtotal": 1200000.00, "account_count": 2 },
    { "profile_id": "<gayan-uuid>",  "name": "Gayan",  "subtotal": 6000.00,    "account_count": 1 },
    { "profile_id": "<vamika-uuid>", "name": "Vamika", "subtotal": 1244000.00, "account_count": 2 }
  ]
}
```

The joint Kotak account (ADR-016: designated `profile_id` = Shweta, `metadata.joint_owners` = [Ketan]) is counted exactly once, inside Shweta's member entry and once inside `family_total` — never double-counted across two member entries. This satisfies Epic 8's "zero leakage — counted exactly once" rule without needing the joint-owners array as a query predicate; it's a display-only attribution concern resolved entirely client-side if ever needed (e.g., "also show on Ketan's card, attributed").

**Individual member drill-down — no separate compute path:** The frontend's "show me just Shweta's accounts" view is a **client-side filter over the already-computed family payload's `members[]` array** — not a separate REST call, not a separate snapshot row, not a separate auth-gated session. Ketan is the only person who ever authenticates; per-member views are a navigation/filter concern inside his one session, never an access-control boundary. This means Epic 8 does not need to build or maintain N separate per-profile snapshot computations once the family rollup exists — the family payload structurally already contains everything a per-member view needs.

**Coexistence with existing per-profile snapshot keys:** The original `WEALTH_NET_WORTH` (singular, per-`profile_id`) key and compute method are **not removed**. They remain mechanically callable (nothing prevents `computeNetWorth(profileId)` for any profile_id), but are no longer the dashboard's primary read path — the gateway's dashboard endpoint serves `..._FAMILY` keys by default once Epic 8 ships. This is a additive, non-breaking introduction of new keys alongside old ones, consistent with ADR-013's "extension pattern: adding a new metric = one new method + one new snapshot key constant, no other changes needed."

**Rejected alternative — flatten to a single combined number with no member structure:** Rejected because the product owner's reference file explicitly nests per-person breakdowns under the family total; flattening would lose data the UI needs for the drill-down requirement and would make a future "also show individually" ask a backward-incompatible schema change instead of a frontend-only filter.

**Rejected alternative — new `household` table/schema as the rollup's identity:** Rejected for the same YAGNI reason ADR-016 already gave for rejecting a household ownership tier — `profile.admin` already models "which profiles belong to this rollup" via the existing `admin_id` FK, with zero new schema. Introducing a new household concept duplicates an FK relationship that already exists.

**Impact on Epic 8 phases:** See `documents/EPIC8_IMPLEMENTATION_PLAN.md` (revised 2026-06-30) — the family-rollup aggregation step is added to Phase 1 as a thin wrapper around the per-account balance fix already in progress; it does not replace or invalidate that work. Phases 3-4 (EMI tracking, goals, validation) reuse the same aggregation mechanism (`listProfiles(admin_id)` + loop + sum-with-nested-breakdown) rather than needing a redesign.

**Schema impact:** None. No new table, no new column, no new migration. `projections.dashboard_snapshot` keeps its existing `(profile_id, snapshot_key)` primary key shape — only new `snapshot_key` string constants are added in `SnapshotKey.java`.

---

## ADR-018: React Query for Frontend Server State

**Status:** Accepted — decided 2026-07-02 (product owner)

**Decision:** Adopt React Query (`@tanstack/react-query`) as the standard mechanism for all server-state data fetching, caching, and refetching in the frontend. Existing `useState`/`useReducer` local component state stays for pure UI state (form inputs, modal open/closed, filter selections). The existing Context API stays for auth/global state (current user, role, active profile). No Redux, no Zustand.

**Context:** PROP-005 asked how the frontend should manage state as cross-domain views (Vacation Planner, Consolidated Action Center) grow beyond what ad-hoc `useEffect` + `useState` data-fetching can comfortably support. Three options were on the table: React Query + local state (Option A), Redux Toolkit (Option B), Zustand (Option C).

**Rationale:**
- The app is overwhelmingly server-data-driven — almost every page's state originates from a domain/gateway REST call, not from complex client-only interaction state. React Query is purpose-built for exactly this shape of app.
- Avoids Redux Toolkit's boilerplate (actions, slices, selectors) for a problem that is fundamentally "fetch, cache, invalidate, refetch" — not general-purpose global state.
- Avoids introducing a second general-state library (Zustand) alongside the Context API already in use for auth — one pattern for server state (React Query) and one pattern for global UI/auth state (Context) is simpler to teach and review than three overlapping systems.
- Directly unblocks the Consolidated Action Center (v0.5 Phase 3): it aggregates alerts from all three domains in one view and needs shared, de-duplicated server-state fetching/caching across pages — a first-class React Query use case (shared query keys, background refetch, no manual cache-busting logic).
- Vacation Planner (v0.5 Phase 2) can adopt the same pattern from the start rather than needing a rewrite once Phase 3 begins.

**Scope of adoption:** New pages (Vacation Planner, Consolidated Action Center) build on React Query from day one. Existing pages are migrated opportunistically — no forced big-bang rewrite of already-working pages.

**Rejected alternatives:**
- **Redux Toolkit:** predictable and has good devtools, but the app has no significant client-only global state beyond auth (already served by Context) — Redux's ceremony isn't justified here.
- **Zustand:** minimal boilerplate, but doesn't solve caching/refetching/invalidation the way React Query does natively; would still need a separate data-fetching layer on top, effectively reinventing React Query.

**Impact:** `web/package.json` gains `@tanstack/react-query` (or `react-query` per whichever major version the team lands on) as a new dependency. A `QueryClientProvider` is added once at the app root (`web/src/App.js` or equivalent). No backend change.
