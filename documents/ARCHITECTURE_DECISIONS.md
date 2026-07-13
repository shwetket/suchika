# Architecture Decisions

| | |
|---|---|
| **Type** | Reference — ADR Log |
| **Audience** | All developers |
| **Status** | Active |
| **Last updated** | 2026-07-13 (ADR-023 **implemented, backend + frontend** — Application Console: process control from `web-gateway` + per-domain `error_log` tables, both scoped exceptions to existing rules, plus the admin-only `/admin/console` frontend page. See ADR-023's "Implementation note" and "Frontend implementation note" for deviations from the original design sketch) |

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
| [ADR-019](#adr-019-profileid-as-a-plain-field-on-domain-entities-adr-006-addendum) | `profileId` as a Plain Field on Domain Entities (ADR-006 addendum) | Accepted — 2026-06-30, documented 2026-07-03 |
| [ADR-020](#adr-020-flyway-consolidation--db-constraint-policy-keep-fkuniquepknot-null-drop-check-only) | Flyway Consolidation & DB Constraint Policy: Keep FK/UNIQUE/PK/NOT NULL, Drop CHECK Only | Accepted — 2026-07-05 |
| [ADR-021](#adr-021-login-auto-attaches-to-the-single-existing-admin-no-client-side-carry-forward) | Login Auto-Attaches to the Single Existing Admin (No Client-Side Carry-Forward) | Accepted — 2026-07-10 |
| [ADR-022](#adr-022-richer-financial-goal-model-additive-walthgoal_plan-tables-not-a-computeformulagoals-rewrite) | Richer Financial Goal Model — Additive `wealth.goal_plan` Tables + Corrected `computeFormulaGoals()` Math + `insurance_policy` | **Implemented — all 3 phases complete 2026-07-12** (`goal_plan` + corrected formulas + `insurance_policy` + real premium wiring + `computeGoalDetail()`/`WEALTH_GOAL_DETAIL_FAMILY` + full Goal Plans/Insurance Policies frontend + Dashboard enrichment) |
| [ADR-023](#adr-023-application-console-process-control-from-web-gateway--per-domain-error_log-tables) | Application Console — Process Control from `web-gateway` + Per-Domain `error_log` Tables | **Implemented — 2026-07-13** (Phase 4 of platform-improvements plan) |

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

**Schema impact:** Requires `wealth.account.metadata JSONB` (did not exist as of v0.4 — only `transaction` and `physical_asset` had a `metadata` column then). Added via Flyway `V6__account_metadata.sql` as part of Epic 8 Phase 1 (complete — see `documents/domain-state/wealth.md`).

**Resolved:** whether joint-account transactions should count toward both owners' *individual* net worth figures or only the designated owner's/household total — resolved as the latter; see ADR-017 (household-level rollup is the dashboard's primary view).

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

**Impact on Epic 8 phases (all complete — see `documents/domain-state/wealth.md`):** the family-rollup aggregation step was added to Phase 1 as a thin wrapper around the per-account balance fix; it did not replace or invalidate that work. Phases 3-4 (EMI tracking, goals, validation) reused the same aggregation mechanism (`listProfiles(admin_id)` + loop + sum-with-nested-breakdown) rather than needing a redesign.

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

---

## ADR-019: `profileId` as a Plain Field on Domain Entities (ADR-006 addendum)

**Status:** Accepted — decided 2026-06-30 (product owner, Q1), documented 2026-07-03 (v0.6 testing-foundation backlog item)

**Decision:** Domain entities are permitted to hold `profileId` as a plain `UUID` field, set via their builder/factory at creation time. This is a deliberate, accepted deviation from ADR-006's stricter wording ("domain entities never store or reason about it") — not an oversight to be fixed.

**Context:** ADR-006 states the `profile_id` filter belongs only in the adapter layer; domain entities should be unaware of it. In practice, seven domain entities across three domains already store `profileId` directly:

| Domain | Entities |
|---|---|
| wealth | `Account`, `PhysicalAsset` |
| health | `VitalReading`, `DoctorVisit` |
| household | `CalendarEvent`, `Goal`, `InventoryItem` |

This was flagged during the v0.4 architect review (Q1): "the current `CalendarEvent` domain entity holds a `profileId` field, set in `CalendarEventService.create()` by passing `profileId` into the domain factory method... The same pattern may apply to other domains" — confirmed here to be all of them except `Transaction` (which is scoped transitively via its parent `Account`) and the `profile` domain's own `Profile`/`Admin` entities (where `profileId`/`adminId` are identity fields, not a tenancy filter, so ADR-006 doesn't apply to them at all).

**Options considered (Q1):**
- **A (chosen):** Keep `profileId` in domain entities as a plain `UUID` field — accept as a pragmatic trade-off, document it, leave ArchUnit rules as-is.
- **B (rejected):** Remove `profileId` from domain entities entirely; pass it as an explicit parameter to every persistence adapter method instead. Tighten ADR-006's wording to match. Add an ArchUnit rule flagging `UUID` fields named `profileId` in `..domain..` packages.

**Rationale for A over B:**
- The isolation guarantee ADR-006 actually cares about — every DB *query* filters by `profile_id` in the adapter layer — is unaffected either way. Whether the entity also happens to carry the same value as a field is orthogonal to whether the repository query predicate includes it.
- Option B is architecturally purer but adds real boilerplate (threading `profileId` as a second parameter through every persistence method call site) for a benefit that is largely aesthetic at this project's current scale (single admin, no auth yet, no adversarial multi-tenant pressure).
- Retrofitting seven already-working, already-tested entities carries real regression risk for a purity improvement with no corresponding bug fix behind it — not a good trade at v0.5/v0.6 scope.
- Matches this project's stated philosophy (`CLAUDE.md`): don't design for hypothetical future requirements; a pragmatic trade-off documented today beats a defensive abstraction with no current payoff.

**What this ADR does NOT change:**
- ADR-006's core rule stands: every adapter-layer DB query must still filter by `profile_id` explicitly. `profileId` living on the entity is a convenience for the domain layer's own use (e.g., a factory method validating a business rule that happens to need the owning profile); it must never be treated as a substitute for the adapter-layer query filter.
- No ArchUnit rule is added to flag or ban this pattern (Option B's proposed rule is explicitly not adopted).

**Revisit trigger:** If a future ArchUnit rule needs to verify `profile_id` presence in adapter query predicates (flagged as a v1.0 item, tracked in `ROADMAP.md`), that rule should inspect the adapter/repository layer directly — it should not use "does the domain entity have a `profileId` field" as a signal, since this ADR establishes that the two are intentionally decoupled.

---

## ADR-020: Flyway Consolidation & DB Constraint Policy: Keep FK/UNIQUE/PK/NOT NULL, Drop CHECK Only

**Status:** Accepted — decided 2026-07-04 (product owner, Q31/Q44-Q46), executed 2026-07-05

**Decision:** Each domain's Flyway migration chain (V1...Vn) is collapsed into one `V1__init_<domain>_consolidated.sql` file, in-place-edited (a one-time exception to "never edit a committed migration," scoped to this consolidation only — normal rule resumes once V1 is committed again). DB-level constraint policy going forward:

- **Keep in DB:** `NOT NULL`, `PRIMARY KEY`, `FOREIGN KEY`, `UNIQUE`.
- **Drop from DB:** `CHECK` constraints — both enum-discriminator CHECKs (already redundant with contract-layer enum validation, ADR-010) and business-rule CHECKs (`amount >= 0`, `value_primary > 0`, date-range checks, `target_amount > 0`, conditional-field rules like "BLOOD_PRESSURE requires value_secondary"). These move to the domain layer as validating static factory methods (`Transaction.create()`, `VitalReading.create()`, `DoctorVisit.create()`, etc.) throwing `IllegalArgumentException`, plus the OpenAPI contract where applicable.
- **VARCHAR name columns capped at `VARCHAR(50)`** project-wide (`account_name`, `institution_name`, `asset_name`, `display_name`, `full_name`) — a standard, not a per-domain judgment call.
- **profileId placement (Q33):** direct `profile_id` column only on each domain's root/primary aggregate table. Child/detail tables unambiguously owned by exactly one already-scoped parent row (`wealth.transaction` via `account_id`, `wealth.statement_upload` via `account_id`, `wealth.upload_error_log` via `upload_id`) do not get their own copy — extends ADR-019's domain-entity-layer reasoning to the schema layer.

**Context:** the original consolidation plan (2026-07-03, since executed and removed from `documents/` — its content is fully superseded by this ADR and the domain-state files) drafted this under an initial instruction to drop FK and CHECK constraints entirely, and UNIQUE along with them. The product owner's actual resolution reversed the FK/UNIQUE removal specifically, while confirming CHECK removal. Rationale for the reversal: FK/UNIQUE catch real bugs (orphaned `profile_id` rows, duplicate natural keys) at zero runtime cost, for free, regardless of application-layer bugs — a materially different risk profile than enum CHECKs, which were already fully redundant with contract validation. CHECK removal for business rules was conditioned on verified-equivalent domain-layer enforcement first — this was audited and closed (see table below).

**Q45 verification (business-rule CHECK → domain-layer equivalent), confirmed 2026-07-05:**

| Dropped CHECK | Domain-layer replacement |
|---|---|
| `wealth.transaction` `amount >= 0` | `Transaction.create()` throws `IllegalArgumentException` |
| `health.vital_reading` `value_primary > 0`, BP-secondary-required | `VitalReading.create()` |
| `health.doctor_visit` `to_date >= from_date`, doctor-name-required-when-visited | `DoctorVisit.create()` |
| `household.goal` `target_amount > 0` | Pre-existing `Goal.create()` (established convention before this consolidation) |
| `household.goal` `current_amount >= 0` | `GoalService.updateCurrentAmount()` guard (added this session) |
| `household.calendar_event` `end_date >= start_date` | Pre-existing `CalendarEvent.create()` |
| `household.inventory_item` `quantity > 0` | Pre-existing `InventoryItem.create()` |

**Execution verification:** Full local `app_db` reset (`scripts/db-reset.ps1`) + fresh migration of all 5 consolidated V1 scripts + full adapter test suite across profile, wealth, health, household, projections — all green, 2026-07-05.

**Rationale for CHECK-only removal (vs. the plan's original FK+CHECK+UNIQUE removal):** Adding a new discriminator or business rule now requires only a domain/contract change, no Flyway migration — the original goal. But referential integrity and natural-key uniqueness are structural invariants worth keeping at the DB layer as a last line of defense; recreating them at the application layer (existence-check REST calls for cross-service FKs, pre-insert uniqueness checks) trades a free, atomic DB guarantee for a slower, non-atomic, easy-to-forget application-layer approximation with real gaps (multi-step delete atomicity, network-call coupling for cross-service checks). Not a good trade at this project's scale.

**Rejected alternative:** Drop FK/UNIQUE too (the original draft direction of the now-executed, since-removed consolidation plan) — rejected per the reasoning above. The accepted-risk table drafted for that alternative (orphaned rows, lost cascade deletes, lost natural-key uniqueness) is superseded by this ADR's final decision and was not carried forward once the plan finished executing.

---

## ADR-021: Login Auto-Attaches to the Single Existing Admin (No Client-Side Carry-Forward)

**Status:** Accepted — decided 2026-07-10

**Decision:** On login, the frontend resolves `admin_id`/`profile_id` by asking the backend "does exactly one admin already exist?" (`GET /v1/admins` via the existing `listAdmins()`), not by matching against a prior `localStorage` session for the same username. Concretely, in `AuthContext.login()`:

1. Call `listAdmins()`.
2. **Exactly one admin, and its `is_active === true`:** auto-attach — set `admin_id` to that admin's id. Then call `listProfiles(adminId, true)` and find the member with `relation_to_admin === 'SELF'`; if `role === 'admin'`, attach that profile's `profile_id` too. This is the household's one and only admin — there is nothing to choose.
3. **Zero admins:** leave `admin_id`/`profile_id` unset. `SetupGate` correctly routes to `/admin/setup`, which creates the first (and, by this app's model, only) admin + SELF profile. True first-run, unchanged from existing behavior.
4. **More than one admin:** do **not** guess, do **not** auto-create a second household, do **not** build a picker UI. Set `household_conflict: true` on the user object instead. `SetupGate` renders a blocking error state ("multiple households found — this app supports exactly one") rather than either redirecting to setup (which would silently spawn a duplicate household) or picking arbitrarily.
5. If the `listAdmins()` call itself fails (network/server error), the exception propagates out of `login()` uncaught — `SignIn.js` already surfaces `err.message` and the user stays on the sign-in screen. This is deliberate: silently treating a transient fetch failure as "zero admins" would risk auto-creating a duplicate household on nothing more than a dropped request.

The previous mechanism — carry forward `admin_id`/`profile_id` from whatever `localStorage.user` blob happened to be sitting in the browser, matched only by username string equality — is removed outright.

**Context — the confirmed bug:** A real household was seeded directly into Postgres (4 profiles under 1 admin) to pilot the app end-to-end. The actual household member could not log into the running frontend and see it:

- `web/src/api/auth.js` `signIn()` calls a backend endpoint that doesn't exist (`/v1/auth/signin`, 404) and silently falls back to a demo stub returning only `{username, role, token, issued_at}` — expected and accepted per ADR-005 (real OIDC auth is deferred to v1.0); not itself the bug.
- The bug was one layer up: `AuthContext.login()` had no backend lookup at all for `admin_id`/`profile_id` — it only "carried forward" those fields from a previous `localStorage` session matching the same username. A fresh browser or a new username has no prior session to carry forward from, so `admin_id` is `undefined`, always.
- Role `admin` + no `admin_id` → `SetupGate` force-redirects to `/admin/setup`, whose step 1 unconditionally calls `createAdmin()` + `createProfile()` — it always creates a **brand-new** admin+profile row. There was no "attach me to the household that already exists" path anywhere in the codebase. Every fresh login as admin silently spawned another empty household.
- Role `user` bypasses `SetupGate` but every page sources `admin_id` from `user.admin_id` for its API calls (v0.5.1 Workstream 2 fix, `profile.md`), so it 400s (`ProfileResource.java` `admin_id is required`) with a raw error surfaced to the user.
- A prior QA pass had "confirmed real data renders correctly" only by hand-patching `localStorage.user.admin_id` in devtools — never exercising a real login path.

**Why auto-attach-to-the-sole-admin is the right direction, not just a stopgap patched over the real fix (checked against roadmap, not assumed):**

- `README.md` states the app is "owned and run locally" — one deployment, one household.
- ADR-017 records the product owner's own framing, stated twice: he is *the only person who ever logs into the app*; other household members are data-attribution targets inside his one session, never independent authenticated users. This is a direct product statement, not an inference.
- `ROADMAP.md`'s only multi-tenant item is "Multi-tenant PostgreSQL (row-level security or per-tenant schemas)" under a distant, unscheduled future features list — about hosting *multiple separate deployments* efficiently, not about one running instance serving multiple households or multiple concurrent admins. It sits alongside "Public domain deployment," "CDN," "99.5% SLA" — infra scaling ambitions, not a near-term product requirement.
- ADR-005 defers all real identity/auth (OIDC/OAuth2, RBAC) to v1.0 and is still unimplemented. Nothing currently maps an arbitrary login username to a specific household member — `SignIn.js` is a free-text username + role dropdown with zero backend credential check.
- Given all of the above, "exactly one admin exists, attach to it" is a correct model of *this app's actual current shape*, not a speculative guess about a future multi-tenant world. It is intentionally cheap to delete once real OIDC auth in v1.0 makes it obsolete — auto-attach is replaced by a real authenticated identity lookup at that point, same shape of change ADR-005 already anticipates.

**Why the fix lives in `AuthContext.login()`, not `SetupGate` or `Setup.js`:**

- `AuthContext` is the app's one established home for auth/global session state (ADR-018 confirms this explicitly — Context API stays for auth/global state, React Query for server data). Identity resolution ("who is this user, which household do they belong to") is a session-establishment concern, squarely inside that responsibility.
- `SetupGate` is a route guard. Its job is "is setup complete, yes/no" — it should stay a dumb boolean check on already-resolved `admin_id`. Teaching it to also resolve identity would duplicate logic that belongs in one place and couple a route guard to backend admin-listing semantics.
- `Setup.js` step 1 is, and remains, "create the first admin." It needs zero changes — it is only ever reached once `admin_id` genuinely doesn't exist anywhere (the zero-admin case), which is the one case it was always correctly built for. Adding an "or attach to an existing household" branch *inside* the wizard would create two divergent code paths solving the same problem in two places.

**The 2-vs-many-admin question:** the current single-household model is enforced, not opened into a picker. Building a multi-admin picker UI now would be scope creep against a scenario nothing in `ROADMAP.md`/`BUSINESS_REQUIREMENTS.md` asks for today, and would quietly legitimize "many households, one app instance" as a supported shape before any of the multi-tenant data-isolation work (row-level security, per-tenant schema routing) that would actually make that safe exists. Surfacing `household_conflict` as a hard-stop error is the honest signal: this state means the data is inconsistent with the app's current single-household assumption and needs a human, not a UI affordance.

**Known pre-existing gap, explicitly not solved by this ADR:** the `role: 'user'` login option has no defined mechanism to map an arbitrary typed username to a specific household member's `profile_id` — there is no username/credential field on `profile.profile` at all. This was already true before this fix and is not introduced by it. Recommended follow-up (product decision, not engineering guesswork): either hide the "User" role option in `SignIn.js` until real per-member auth exists in v1.0, or explicitly scope what a "user" session is allowed to do without a `profile_id`. Tracked as an open issue in `documents/domain-state/profile.md` rather than guessed at here.

**Rejected alternatives:**
- **Keep localStorage carry-forward, just fix the matching key:** rejected — any client-side-only heuristic fails identically on a fresh browser/device, which is the exact failure mode that surfaced this bug. The backend, not the browser, is the source of truth for "does this household already exist."
- **Auto-attach silently even when `listAdmins()` returns >1:** rejected — picking arbitrarily (e.g., first admin returned) risks silently attaching a real user to the wrong household's financial/health data. Wrong-tenant data exposure is a worse failure mode than a blocked login screen.
- **Solve it in `Setup.js` with an "attach to existing household" step added to the wizard:** rejected — this duplicates identity-resolution logic in the onboarding UI instead of the auth layer, and still requires `AuthContext`/`SetupGate` to somehow know not to redirect there in the first place, which is circular.

**Impact:** `web/src/context/AuthContext.js` (`login()` rewritten, carry-forward block removed), `web/src/components/SetupGate.js` (new `household_conflict` branch), no backend change (`listAdmins()`/`listProfiles()` already exist and already support this). No new Flyway migration, no contract change.

**Supersedes:** The DB constraint philosophy previously documented in `CLAUDE.md` prior to 2026-07-05 (which additionally allowed business-rule CHECKs like `amount >= 0` to stay in the DB). This ADR's policy is now the one recorded in `CLAUDE.md`'s "DB constraint philosophy" section.

---

## ADR-022: Richer Financial Goal Model — Additive `wealth.goal_plan` Tables, Not a `computeFormulaGoals()` Rewrite

**Status:** **Implemented — all 3 phases complete 2026-07-12.** Phase 1 (`wealth.goal_plan` + 3 child tables, corrected `computeFormulaGoals()`, `ExpenseCategory` widened), Phase 2 (`wealth.insurance_policy` (`V5__insurance_policy.sql`) + full domain/ports/adapters CRUD vertical slice, `THIRTY_SEVENTY_TARGET`'s insurance-premiums term wired to real active-policy data, monthly-normalized — ANNUAL ÷ 12, MONTHLY pass-through — replacing the Phase 1 hardcoded-0 placeholder), and Phase 3 (`computeGoalDetail()`/`WEALTH_GOAL_DETAIL_FAMILY` — the milestone/rule/trigger-event merge step matching each `goal_plan` row to its live `WEALTH_FORMULA_GOALS_FAMILY` entry, including `INSURANCE_FREE`'s "WITH insurance" raw-list attachment; plus the full Goal Plans and Insurance Policies frontend CRUD UI; plus Dashboard milestone/rule enrichment with a tested graceful fallback) are all implemented. See `documents/domain-state/wealth.md`'s Implementation Status table for the full build record of all 3 phases. Everything below this line is the original v2 design proposal (2026-07-11), kept as-is for history; where the shipped implementation made a concrete simplification (e.g. `goal_plan.detail`/`insurance_policy.payout_structure` as a flat `Map<String,String>` rather than nested JSON), that is noted in the domain-state file, not retrofitted into this text.

### What changed from v1, and why

v1 assumed `computeGoalDetail()` could merge `goal_plan` milestones straight onto whatever `current_value` `computeFormulaGoals()` already emits per goal_id. **That assumption is wrong.** The product owner reviewed v1 and found the real flaw: at least 3 of the 5 shipped formulas measure the wrong thing entirely — not "less rich," a different metric under the same name. Example: shipped `DEBT_CROSSOVER` is `monthly EMI ÷ net worth`; the real Debt Crossover metric is `MF corpus ÷ outstanding debt`. Same goal_id, same card, unrelated math. Merging milestones onto the wrong number would have shipped a broken UI silently — v1's merge design was correct, its input wasn't.

**Resolution: `computeFormulaGoals()`'s 5 formulas are corrected in place. This is a bugfix to existing shipped logic**, not a new parallel system — same 5 goal_ids, same `WEALTH_FORMULA_GOALS_FAMILY` snapshot key, same Dashboard Household Goals card, same `goal_plan`/`goal_plan_milestone`/additive-schema shape from v1. Once the live math is right, v1's merge-by-goal_id design in `computeGoalDetail()` becomes valid — not because of a generic assumption, but because each formula now actually shares its milestone's unit.

Everything else new below (insurance_policy, per-child YEAR_ONE, income-category transactions, milestone checklist mode) is the product owner resolving gaps v1 had flagged as open (Postgres NULL-uniqueness, checklist milestones, real insurance-vs-no-insurance comparison) or hadn't scoped yet (per-child education goals).

### The 5 corrected formulas — old (wrong) vs new (real), one table

| Goal ID | Shipped today (wrong) | Corrected (product owner's real definition) | Unit / achieved direction |
|---|---|---|---|
| `DEBT_CROSSOVER` | `totalMonthlyEmi ÷ familyNetWorth × 100`, achieved when `< threshold%` (default 50) | `(family MF corpus, SELF+SPOUSE profiles only, excludes CHILD-relation accounts) ÷ (total outstanding balance across HOME_LOAN/PERSONAL_LOAN/CAR_LOAN accounts, **excluding CHILD-relation loans too — confirmed 2026-07-11**) × 100` | percent, achieved `>= 100` (**direction flips** — was the one `<` exception, now behaves like the majority) |
| `THIRTY_SEVENTY_TARGET` | `LIQUID tier ÷ total liquidity × 100`, achieved when `>= 30%` | `(EMI total [existing WEALTH_EMI_TRACKING_FAMILY.total_monthly_emi] + non-discretionary DEBIT txns [HOUSEHOLD_CORE/CHILD_RELATED/MAINTENANCE categories] + insurance premiums [new insurance_policy table]) ÷ (trailing 3-month avg of income-tagged CREDIT txns) × 100` | percent, achieved `<= 30` (**becomes the new sole exception** — everything else is `>=`) |
| `FREEDOM_RUNWAY` | `(LIQUID + SEMI_LIQUID tiers) ÷ monthlyBudgetCap`, achieved `>= freedom_runway_months` (default 6) | same shape, but core-runway-capital composition audited/fixed (see below); target now `360` | months, achieved `>=` (unchanged direction) |
| `INSURANCE_FREE` | `totalInvestmentValue >= annualIncome × insurance_multiple` | `(MaxGain-purpose_tag + FD-type account balances) ÷ (outstanding debt [WEALTH_EMI_TRACKING_FAMILY.total_outstanding_balance] + legal_fees [new policy_settings key] + academic_buffer [new policy_settings key]) × 100` | percent, achieved `>= 100` (unchanged direction, changed formula) |
| `YEAR_ONE` | household-level: `familyNetWorth >= yearOneAnnualTarget` | **per child now, not household**: for each CHILD-relation profile with >=1 active MUTUAL_FUND account: `(that child's MUTUAL_FUND account balances) ÷ (25% × future_cost)`, `future_cost = base_cost × (1 + inflation_rate)^years_to_entry` × 100 | percent, achieved `>=` (unchanged direction; entry now repeats per child instead of appearing once) |

**Why the direction table matters, concretely:** v1's `computeGoalDetail()` design hardcoded "`<` for `DEBT_CROSSOVER`, `>=` for the other four" as the achieved-direction predicate for milestone comparison. That hardcoded exception is now wrong on both ends — `DEBT_CROSSOVER` becomes `>=` and `THIRTY_SEVENTY_TARGET` becomes the new `<=` exception. `computeFormulaGoals()`'s and `computeGoalDetail()`'s achieved-direction logic must both become an explicit per-goal-type lookup (a small `Map<String, Comparator-ish>` or switch), never a single "except goal X" special case — the single-exception shape is exactly what silently broke once the exception moved. This is the concrete code consequence of the flaw, not just a formula edit.

**`FREEDOM_RUNWAY` core-runway-capital audit finding:** current composition is `LIQUID + SEMI_LIQUID` tiers from `computeLiquidityTiers()`, which already only reads `wealth.account` rows (real estate/gold live in `wealth.physical_asset`, a different table never read by this step, and gratuity has no schema representation at all — all three are excluded today by omission, not by design intent, but the outcome matches the product owner's exclusion list). **Two real gaps found and must be fixed:** (1) `PPF`-type accounts have no structural exclusion — if an admin tags a PPF account's `metadata.liquidity_tier` as `SEMI_LIQUID`, it counts today, and PPF is on the exclusion list; (2) the tier loop iterates every household member with no relation filter — a CHILD-relation FD or MUTUAL_FUND account tagged LIQUID/SEMI_LIQUID counts today, and children's FD/MF are both explicitly excluded. Fix: `FREEDOM_RUNWAY`'s reading of the tier totals must additionally filter out `account_type = PPF` and `relation_to_admin = CHILD` accounts at aggregation time — either a new parallel "core runway capital" aggregation (mirroring `accumulateTiersForMember` but with the 2 extra exclusions) or a 4th field alongside the existing tier totals. Recommend the former — reusing `WEALTH_LIQUIDITY_TIERS_FAMILY.tiers` as-is for `FREEDOM_RUNWAY` is no longer correct once these 2 exclusions exist, so it needs its own aggregation, not the shared one. `policy_settings.freedom_runway_months` is confirmed genuinely admin-settable today (`PATCH /v1/admins/{adminId}/policy`, Epic 8 Phase 4) — only the default changes (6 → 360), no code needed for the target itself, just an admin data-entry action.

**`policy_settings` fallout — 3 of 5 keys go dead:** `debt_crossover_threshold_percent` (was the DEBT_CROSSOVER threshold, now a fixed 100), `insurance_multiple` (was INSURANCE_FREE's `× annual income` multiplier, replaced by debt + legal fees + academic buffer), and `year_one_annual_target` (was the household-level YEAR_ONE target, replaced by per-child education-cost math) are no longer read by the corrected formulas. They stay in the `policy_settings` JSONB schema (harmless, no migration needed to remove a JSONB key) but become dead config — `PolicySettings.js`'s admin form should eventually drop those 3 fields. Flagged as follow-up cleanup, out of scope for this ADR. **2 new keys added** to the same JSONB, zero migration: `insurance_free_legal_fees`, `insurance_free_academic_buffer` (both admin-entered NUMERIC-ish values feeding `INSURANCE_FREE`'s new denominator) — same `PATCH /v1/admins/{adminId}/policy` endpoint, no new endpoint needed.

**`THIRTY_SEVENTY_TARGET` needs a new income-category concept on CREDIT transactions.** `ExpenseCategory` (`application/domain/wealth/domain/src/main/java/com/suchika/wealth/domain/ExpenseCategory.java`) is a plain Java enum (not a SQL enum — stored as a string in `transaction.metadata.category` JSONB, no CHECK constraint, per ADR-010) with 5 DEBIT-shaped values today: `HOUSEHOLD_CORE, CHILD_RELATED, MAINTENANCE, DISCRETIONARY, UNCATEGORIZED`. Confirmed by reading `TransactionService.updateCategory()`: it never validates category against `txn_type` today — any category can already be set on any transaction, DEBIT or CREDIT, at the domain layer. So this is **not zero code** as the product owner's framing hedged — it needs: (1) widen the `ExpenseCategory` enum to 8 values, adding `SALARY, RENTAL, OTHER_INCOME`; (2) widen `wealth.yaml`'s `ExpenseCategory` schema enum list to match. No DB migration (still an unconstrained string column), no new validation logic (there wasn't any to begin with). `computeFormulaGoals()`'s new `THIRTY_SEVENTY_TARGET` step must page through each member's accounts' transactions (existing `listTransactions(accountId, profileId, from, to, txnType, page, size)` call, already supports date-range + txn_type filtering) for the trailing 3-month window, filtering DEBIT rows to the 3 non-discretionary categories and CREDIT rows to the 3 income categories client-side (no `category` query param exists on `listTransactions` — filtering happens in the gateway after fetch, same pattern `computeLiquidityTiers`/`computeEmiTracking` already use for per-account JSON aggregation). Flagged as a perf watch-item below, not blocking.

**`YEAR_ONE` per-child needs education-cost inputs with no natural home except `goal_plan` — a deliberate, scoped exception to the "live math never depends on `goal_plan`" separation.** `future_cost = base_cost × (1 + inflation_rate)^years_to_entry` needs 3 admin-entered numbers *per child*. They can't live in `policy_settings` (that's household-flat, not per-member — `profile`-domain owned besides). They fit `goal_plan`'s per-row shape naturally once `beneficiary_profile_id` exists (see schema below) — matching the precedent v1 already set for `assumed_growth_rate` (a flat nullable numeric column on `goal_plan`, meaningful only to some goal types). So: **`computeFormulaGoals()` gains a new, YEAR_ONE-only call to `WealthServiceClient.listGoalPlans(adminId)`** to enumerate configured per-child rows and read their education inputs — the one deliberate exception to v1's "computeFormulaGoals and goal_plan are fully decoupled" claim. Consequence: a CHILD profile with no `goal_plan` row (`goal_type=YEAR_ONE`, `beneficiary_profile_id=<child>`) configured yet simply has no YEAR_ONE entry in `WEALTH_FORMULA_GOALS_FAMILY` at all — `achieved_count`/`total_count` on that snapshot become `4 + (number of children with a configured YEAR_ONE goal_plan row)`, not a fixed `5`. This is a real, documented shape change to a live, shipped payload (`total_count` is no longer always 5) — Dashboard.js's Household Goals card must not assume `total_count === 5`; check before implementation.

**Decision:** The product owner's "goal theory" documents (objective, baseline, target state, milestones, rules, step-up triggers, and — for one goal — a 5-phase execution protocol) for the same 5 Epic 8 formula goals (`DEBT_CROSSOVER`, `THIRTY_SEVENTY_TARGET`, `FREEDOM_RUNWAY`, `INSURANCE_FREE`, `YEAR_ONE`) are modeled as **new, additive tables in the `wealth` schema** (`wealth.goal_plan` + 3 child tables + new `wealth.insurance_policy`), read by a **new gateway compute step** that writes a **new snapshot key** (`WEALTH_GOAL_DETAIL_FAMILY`). **v2 change:** `ProjectionCalculationEngine.computeFormulaGoals()` and `WEALTH_FORMULA_GOALS_FAMILY` **are modified** — the 5 formulas inside are wrong today (see corrected-formulas table above) and are corrected in place as a bugfix, same goal_ids/snapshot key/Dashboard consumer. v1's "not modified" claim is retracted; the additive-tables decision for `goal_plan`/its children/`insurance_policy` is unchanged.

**Why additive, not in-place extension of `computeFormulaGoals()`/`policy_settings`:**

- The 5 goal IDs are already a de facto closed set hardcoded as Java string literals inside `computeFormulaGoals()` (`buildGoalEntry("DEBT_CROSSOVER", ...)` etc.) — there is no generic goal-definition loop to extend, only 5 inline blocks of bespoke math. Reshaping that method to also emit milestones/rules/objective text means it now has two different data lifecycles tangled into one write path: **live math recomputed every refresh** (current_value, from account/EMI/liquidity snapshots) vs. **static admin-authored config that changes rarely** (objective paragraph, milestone labels, rule text). Keeping them apart matches how every other Epic 8 phase shipped — EMI tracking, liquidity tiers, and growth projection were each added as their *own* compute step/snapshot key rather than folded into an earlier one (ADR-013's own "extension pattern").
- `WEALTH_FORMULA_GOALS_FAMILY` is live, tested, and consumed today by the Dashboard's Household Goals card (`Dashboard.js`). Changing its payload shape risks a regression in a shipped, live-verified feature for zero functional gain — a new key is strictly safer and is exactly the pattern ADR-013 documents as the intended extension mechanism ("adding a new metric = one new method + one new snapshot key constant, no other changes needed").
- `policy_settings` (Q23) was justified as "admin-scoped, rarely-changes config lives near the identity layer" for a handful of flat numeric thresholds. The richer model is materially heavier — per-goal objective/baseline/milestones/rules/triggers, one goal with a 5-phase sub-structure — and is wealth-domain vocabulary (loan balances, liquidity tiers, SIP, insurance multiples), not identity-layer config. It belongs in `wealth`, not bolted onto `profile.admin.policy_settings` as an ever-growing nested JSON blob with no query surface.

**Schema — `wealth.goal_plan` + 3 child tables (sketch, `V4__goal_plan.sql`, additive) + new `wealth.insurance_policy` (sketch, `V5__insurance_policy.sql`, additive, separate file since it's a separate concern):**

**v2 changes vs v1's DDL:** `goal_plan` gains nullable `beneficiary_profile_id` (per-child `YEAR_ONE` rows) and 3 nullable education-input columns (see above — needed by *live* `computeFormulaGoals()` math for `YEAR_ONE`, not just the richer detail view, hence typed columns not buried in `detail` JSONB, same precedent as `assumed_growth_rate`); its uniqueness constraint is corrected (see NULL-uniqueness note below — v1's constraint as literally written does not enforce what v1 claimed). `goal_plan_milestone` gains nullable `target_value`, `is_manual_checklist`, `is_achieved`.

```sql
-- V4__goal_plan.sql
CREATE TABLE wealth.goal_plan (
    id                        UUID          NOT NULL DEFAULT gen_random_uuid(),
    admin_id                  UUID          NOT NULL,
    goal_type                 VARCHAR(50)   NOT NULL,
    beneficiary_profile_id    UUID,                        -- NEW v2: nullable; non-null only for YEAR_ONE (one row per child)
    objective                 TEXT          NOT NULL,
    target_state               TEXT,
    assumed_growth_rate       NUMERIC(7,4),
    education_base_cost       NUMERIC(19,4),                -- NEW v2: YEAR_ONE-only, NULL for other 4 goal types
    education_inflation_rate  NUMERIC(7,4),                 -- NEW v2: YEAR_ONE-only
    education_years_to_entry  INTEGER,                      -- NEW v2: YEAR_ONE-only
    detail                    JSONB         NOT NULL DEFAULT '{}'::jsonb,
    is_active                 BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at                TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at                TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_goal_plan PRIMARY KEY (id),
    CONSTRAINT fk_goal_plan_admin FOREIGN KEY (admin_id)
        REFERENCES profile.admin(id) ON DELETE RESTRICT,
    CONSTRAINT fk_goal_plan_beneficiary FOREIGN KEY (beneficiary_profile_id)
        REFERENCES profile.profile(id) ON DELETE RESTRICT,
    -- CORRECTED v2 (see "Postgres NULL-uniqueness" note below) — NULLS NOT DISTINCT
    -- (Postgres 15+; this project runs 16) makes the 4 singleton goal types' NULL
    -- beneficiary_profile_id collide with each other (singleton enforced), while
    -- YEAR_ONE rows differ on a real non-null beneficiary_profile_id (multiple rows allowed).
    CONSTRAINT uq_goal_plan_admin_type_beneficiary
        UNIQUE NULLS NOT DISTINCT (admin_id, goal_type, beneficiary_profile_id)
);
CREATE INDEX idx_goal_plan_admin ON wealth.goal_plan(admin_id);
CREATE INDEX idx_goal_plan_beneficiary ON wealth.goal_plan(beneficiary_profile_id) WHERE beneficiary_profile_id IS NOT NULL;

CREATE TABLE wealth.goal_plan_milestone (
    id                  UUID          NOT NULL DEFAULT gen_random_uuid(),
    goal_plan_id        UUID          NOT NULL,
    sequence_no         INTEGER       NOT NULL,
    label               VARCHAR(50)   NOT NULL,
    target_value        NUMERIC(19,4),                     -- CHANGED v2: nullable (skipped when is_manual_checklist)
    is_manual_checklist BOOLEAN       NOT NULL DEFAULT FALSE, -- NEW v2
    is_achieved         BOOLEAN       NOT NULL DEFAULT FALSE, -- NEW v2: admin-toggled directly for checklist items;
                                                               -- for non-checklist items this is DERIVED at read time by
                                                               -- computeGoalDetail() (current_value vs target_value) and
                                                               -- overwritten on every refresh — see PATCH endpoint note below
    significance        TEXT          NOT NULL,
    CONSTRAINT pk_goal_plan_milestone PRIMARY KEY (id),
    CONSTRAINT fk_milestone_goal_plan FOREIGN KEY (goal_plan_id)
        REFERENCES wealth.goal_plan(id) ON DELETE CASCADE,
    CONSTRAINT uq_milestone_sequence UNIQUE (goal_plan_id, sequence_no)
);

CREATE TABLE wealth.goal_plan_rule (
    id            UUID          NOT NULL DEFAULT gen_random_uuid(),
    goal_plan_id  UUID          NOT NULL,
    sequence_no   INTEGER       NOT NULL,
    rule_name     VARCHAR(50)   NOT NULL,
    rule_text     TEXT          NOT NULL,
    CONSTRAINT pk_goal_plan_rule PRIMARY KEY (id),
    CONSTRAINT fk_rule_goal_plan FOREIGN KEY (goal_plan_id)
        REFERENCES wealth.goal_plan(id) ON DELETE CASCADE,
    CONSTRAINT uq_rule_sequence UNIQUE (goal_plan_id, sequence_no)
);

CREATE TABLE wealth.goal_plan_trigger_event (
    id                 UUID          NOT NULL DEFAULT gen_random_uuid(),
    goal_plan_id       UUID          NOT NULL,
    sequence_no        INTEGER       NOT NULL,
    event_name         VARCHAR(50)   NOT NULL,
    trigger_condition  TEXT          NOT NULL,
    resulting_change   TEXT          NOT NULL,
    CONSTRAINT pk_goal_plan_trigger_event PRIMARY KEY (id),
    CONSTRAINT fk_trigger_goal_plan FOREIGN KEY (goal_plan_id)
        REFERENCES wealth.goal_plan(id) ON DELETE CASCADE,
    CONSTRAINT uq_trigger_sequence UNIQUE (goal_plan_id, sequence_no)
);
```

```sql
-- V5__insurance_policy.sql (NEW v2, separate migration file — separate concern from goal_plan)
CREATE TABLE wealth.insurance_policy (
    id                UUID          NOT NULL DEFAULT gen_random_uuid(),
    admin_id          UUID          NOT NULL,               -- household-level, not per-member — matches goal_plan/policy_settings
    policy_name       VARCHAR(50)   NOT NULL,
    provider          VARCHAR(50)   NOT NULL,
    policy_type       VARCHAR(50)   NOT NULL,                -- e.g. TERM/GROUP_TERM/INVESTMENT_LINKED/ENDOWMENT/HEALTH; no SQL enum (ADR-010)
    premium_amount    NUMERIC(19,4) NOT NULL,
    premium_frequency VARCHAR(20)   NOT NULL,                -- e.g. MONTHLY/ANNUAL; no SQL enum
    coverage_amount   NUMERIC(19,4),                          -- nullable: some policies are income-stream, not lump-sum
    payout_structure  JSONB         NOT NULL DEFAULT '{}'::jsonb, -- heterogeneous per policy_type — lump sum / escalating
                                                                    -- monthly income / sum-assured-at-maturity; same
                                                                    -- escape-hatch precedent as goal_plan.detail
    is_active         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT pk_insurance_policy PRIMARY KEY (id),
    CONSTRAINT fk_insurance_policy_admin FOREIGN KEY (admin_id)
        REFERENCES profile.admin(id) ON DELETE RESTRICT
);
CREATE INDEX idx_insurance_policy_admin ON wealth.insurance_policy(admin_id);
```

No `CHECK` constraints anywhere (ADR-020) — only `NOT NULL`/`PK`/`FK`/`UNIQUE`. All name-ish columns (`label`, `rule_name`, `event_name`, `policy_name`, `provider`) capped at `VARCHAR(50)` project-wide. `goal_type`/`policy_type`/`premium_frequency` are plain `VARCHAR`, no SQL enum (ADR-010) — soft-validated at the contract layer.

**Postgres NULL-uniqueness — v1's constraint was wrong, corrected here.** The product owner's stated resolution ("plain `UNIQUE(admin_id, goal_type, beneficiary_profile_id)`, NULLs are distinct, so YEAR_ONE gets multiple rows while the other 4 stay singleton") is **half right and half a bug.** Standard Postgres `UNIQUE` (pre-15 default, and still the default in 16 unless you opt in) treats every `NULL` as distinct from every other `NULL` — that's exactly what makes multiple `YEAR_ONE` rows work (different real `beneficiary_profile_id` values are distinct — fine either way). But it *also* means two `DEBT_CROSSOVER` rows for the same admin, both with `beneficiary_profile_id = NULL`, do **not** collide under plain `UNIQUE` — `NULL <> NULL`, so the constraint silently allows duplicate singleton-type rows, which breaks the "at most one row" invariant the product owner explicitly wants for the other 4 types. **Fix used above:** `UNIQUE NULLS NOT DISTINCT (admin_id, goal_type, beneficiary_profile_id)` — a Postgres 15+ feature (confirmed available; this project runs `postgres:16` per `.devcontainer/docker-compose.yml`/CI). Under `NULLS NOT DISTINCT`, two rows with the same `(admin_id, goal_type, NULL)` collide (singleton enforced correctly for the 4 non-`YEAR_ONE` types) while two `YEAR_ONE` rows with *different* non-null `beneficiary_profile_id` values still don't collide (per-child rows still allowed) — this single constraint gets both halves right, where the plain-`UNIQUE` version the product owner described only gets one half right. Confirmed this is the correct, minimal fix — no need for two partial unique indexes.

**Milestones/rules/trigger events are structured tables; baseline + the Insurance-Free 5-phase protocol go in `goal_plan.detail` JSONB.** Baseline metrics genuinely vary in shape per goal type (Debt Crossover: loan balances + MF corpus; Freedom Runway: a different asset basket + monthly survival number) — same "heterogeneous per-type shape" justification already used for `account.metadata`/`physical_asset.metadata`/now `insurance_policy.payout_structure`. The 5-phase emergency execution protocol is unique to one goal and doesn't generalize — forcing it into the milestone/rule shape would be a worse fit than the escape hatch this project already has precedent for. `detail` is intentionally opaque to SQL — never queried by column, only round-tripped.

**`is_manual_checklist` milestones — resolved: keep the existing bulk-`PUT` for authoring, add one new single-milestone `PATCH` for the achieved toggle only.** Two different operations were tangled in the product owner's ask: (1) *authoring* a milestone (label/target_value/sequence/is_manual_checklist) — rare, done as a batch, the existing `PUT /goal-plans/{id}/milestones` bulk-replace is the right tool, unchanged. (2) *toggling `is_achieved`* on a checklist item — frequent, single-field, admin clicks one checkbox. Forcing (2) through bulk-`PUT` means resending the entire milestone array (labels, sequence, other items' state) for a one-checkbox click — wasteful, and a real risk: two admins toggling different checklist items concurrently would race and clobber each other's bulk-PUT. **Resolution:** add `PATCH /goal-plans/{id}/milestones/{milestoneId}` (new, single-field: `{ is_achieved: boolean }`) — same convention already established by `PATCH /accounts/{accountId}/transactions/{txnId}/category` (single-field PATCH alongside a bulk endpoint). Only meaningful when `is_manual_checklist = true`; for non-checklist milestones `is_achieved` is derived by `computeGoalDetail()` every refresh (current_value vs target_value) and this PATCH endpoint should reject the call (400) if `is_manual_checklist = false` on that milestone — an admin toggling a formula-derived milestone by hand would be silently overwritten on the next dashboard refresh anyway, better to reject than let it looks like it worked.

**Milestone/rule/trigger-event text is policy narrative, not code-enforced (unchanged from v1).** Per the product owner's own framing, rules ("No Liquidation", "SIP is Sacred") are household discipline, surfaced in the UI — the system does not block a liquidation or a skipped SIP. Trigger events' `trigger_condition` stays free `TEXT` for the same reason UC 8.3 (dynamic triggers) was deliberately left unbuilt (see `documents/domain-state/wealth.md`) — this ADR does not resurrect it.

**Milestone status logic (v2 correction): the achieved-direction predicate is now an explicit per-goal-type lookup, not a single hardcoded exception.** v1 hardcoded "`<` for `DEBT_CROSSOVER`, `>=` for the other four" directly in prose and (implied) in code. That's now wrong on both ends — the corrected formulas make `DEBT_CROSSOVER` an `>=` goal and `THIRTY_SEVENTY_TARGET` the new `<=` exception (see the corrected-formulas table above). `computeGoalDetail()` (for milestone status) and `computeFormulaGoals()` (for the goal's own `ACHIEVED`/`IN_PROGRESS` status) must both read direction from one shared, explicit table: `{THIRTY_SEVENTY_TARGET: <=, everything else: >=}`. This is a small, deliberate lookup precisely so the next formula correction doesn't require another silent hunt for a hardcoded exception. Still assumes milestones are checkpoints on the same metric/unit as the parent goal's `current_value` — true now that the underlying formulas are fixed to match their own goal's milestones (the flaw this whole revision exists to fix). `is_manual_checklist = true` milestones skip this comparison entirely — their `is_achieved` is admin-toggled (see PATCH endpoint above), never derived.

**Scoping — `admin_id`, not `profile_id` (deliberate ADR-006 extension, same shape as `policy_settings`), now covering 5 tables including `insurance_policy`:** Goal plans and insurance policies are household-level, not per-member — identical reasoning to ADR-017 (formula goals have no per-member variant) and to `policy_settings` living on `profile.admin`. `wealth.goal_plan.admin_id`/`wealth.insurance_policy.admin_id` are direct FKs to `profile.admin(id)` — the identity-anchor FK convention every domain already uses, one level up the identity hierarchy. Every adapter-layer query on all 5 tables must filter by `admin_id` — same ADR-006 spirit, keyed by the household unit instead of the member unit. `goal_plan.beneficiary_profile_id` is a second, narrower scope dimension layered on top (per-child, only for `YEAR_ONE`) — adapters filtering `goal_plan` by child must filter by both `admin_id` (household) and `beneficiary_profile_id` (which child), never `beneficiary_profile_id` alone, since a stray cross-household child id must never leak another household's goal_plan row.

**Contract sketch — `wealth.yaml` (new domain-owned CRUD, admin-scoped). v2 adds `/insurance-policies`, `beneficiary_profile_id` + 3 education fields on `GoalPlan`, nullable `target_value` + `is_manual_checklist` + `is_achieved` on `GoalMilestone`, and the new single-milestone `PATCH`:**

```yaml
/goal-plans:
  get:  { parameters: [admin_id], responses: { 200: GoalPlan[] } }
  post: { parameters: [admin_id], requestBody: CreateGoalPlanRequest }   # CreateGoalPlanRequest gains beneficiary_profile_id (nullable)
/goal-plans/{id}:
  get: {}
  patch:  { requestBody: UpdateGoalPlanRequest }   # objective/target_state/assumed_growth_rate/education_*/detail/is_active
  delete: {}                                        # soft-delete, is_active=false
/goal-plans/{id}/milestones:
  put: { requestBody: GoalMilestone[] }             # bulk replace, ordered — matches "authored as one document"
/goal-plans/{id}/milestones/{milestoneId}:            # NEW v2
  patch: { requestBody: { is_achieved: boolean } }   # single-field toggle, is_manual_checklist milestones only (400 otherwise)
/goal-plans/{id}/rules:
  put: { requestBody: GoalRule[] }
/goal-plans/{id}/trigger-events:
  put: { requestBody: GoalTriggerEvent[] }
/insurance-policies:                                   # NEW v2 — same CRUD pattern as /goal-plans
  get:  { parameters: [admin_id], responses: { 200: InsurancePolicy[] } }
  post: { parameters: [admin_id], requestBody: CreateInsurancePolicyRequest }
/insurance-policies/{id}:                              # NEW v2
  get: {}
  patch:  { requestBody: UpdateInsurancePolicyRequest }
  delete: {}                                            # soft-delete, is_active=false

GoalType: { type: string, enum: [DEBT_CROSSOVER, THIRTY_SEVENTY_TARGET, FREEDOM_RUNWAY, INSURANCE_FREE, YEAR_ONE] }
GoalPlan: { id, admin_id, goal_type: GoalType, beneficiary_profile_id: {type: string, format: uuid, nullable: true},
            objective, target_state, assumed_growth_rate,
            education_base_cost: {nullable: true}, education_inflation_rate: {nullable: true},
            education_years_to_entry: {type: integer, nullable: true},
            detail: object, is_active, milestones: GoalMilestone[], rules: GoalRule[],
            trigger_events: GoalTriggerEvent[], created_at, updated_at }
GoalMilestone: { id, label: {maxLength: 50}, target_value: {nullable: true}, is_manual_checklist: boolean,
                 is_achieved: boolean, significance }
GoalRule: { rule_name: {maxLength: 50}, rule_text }
GoalTriggerEvent: { event_name: {maxLength: 50}, trigger_condition, resulting_change }

PolicyType: { type: string, enum: [TERM, GROUP_TERM, INVESTMENT_LINKED, ENDOWMENT, HEALTH] }
PremiumFrequency: { type: string, enum: [MONTHLY, ANNUAL] }
InsurancePolicy: { id, admin_id, policy_name: {maxLength: 50}, provider: {maxLength: 50}, policy_type: PolicyType,
                   premium_amount, premium_frequency: PremiumFrequency, coverage_amount: {nullable: true},
                   payout_structure: object, is_active, created_at, updated_at }
CreateInsurancePolicyRequest: { policy_name, provider, policy_type, premium_amount, premium_frequency,
                                 coverage_amount: {nullable: true}, payout_structure: {nullable: true} }
```

Bulk-`PUT`-replace stays for milestones/rules/trigger-events as *authoring* operations (unchanged from v1's reasoning). `ExpenseCategory` contract schema (existing, not new) widens from 5 to 8 enum values: `HOUSEHOLD_CORE, CHILD_RELATED, MAINTENANCE, DISCRETIONARY, UNCATEGORIZED, SALARY, RENTAL, OTHER_INCOME` — no `txn_type` restriction added at the contract level either (matches the domain layer, which never had one).

**`gateway.yaml` touch points:** pure `JsonNode`-passthrough proxies for all `/goal-plans/...` and `/insurance-policies/...` paths, same convention `WealthGatewayResource` already uses for physical assets — no new Java shape needed in the gateway for CRUD. The **computed/enriched read** (progress + milestone status merged with config) needs **no new REST path at all** — it is the `WEALTH_GOAL_DETAIL_FAMILY` snapshot key served through the existing `GET /v1/projections/dashboard/{profileId}` read path, same as every other `_FAMILY` key.

**New gateway compute step:** `ProjectionCalculationEngine.computeGoalDetail(UUID profileId)`, added to `refreshAll()` after `computeFormulaGoals()` (needs its output already in the same-pass snapshot map, same `loadSnapshotsAsMap()` pattern). For each `wealth.goal_plan` row (`WealthServiceClient.listGoalPlans(adminId)`), match into `WEALTH_FORMULA_GOALS_FAMILY.goals[]` — by `goal_id == goal_type` for the 4 singleton types, by `goal_id == goal_type AND beneficiary_profile_id` for `YEAR_ONE` (goals[] entries now carry `beneficiary_profile_id` for `YEAR_ONE`, see below) — merge objective/target_state/milestones/rules/trigger_events/detail with the live current_value/target_value/status, compute per-milestone status using the shared achieved-direction lookup (see above), and write the merged array to `SnapshotKey.WEALTH_GOAL_DETAIL_FAMILY`. For `INSURANCE_FREE`, also read `WealthServiceClient.listInsurancePolicies(adminId)` and attach the raw policy list to that goal's detail payload as the "WITH insurance" comparison (see the aggregation caveat flagged below — v1 raw list, not a single blended number, for this revision). Goal rows with no configured `goal_plan` are omitted from this richer payload; for the 4 singleton types the base `WEALTH_FORMULA_GOALS_FAMILY` card is unaffected either way (still shows all 4). For `YEAR_ONE`, a child with no `goal_plan` row has **no entry anywhere** — not in the richer payload, and not in the base formula-goals card either, since (per the earlier note) `computeFormulaGoals()` itself now depends on `goal_plan` to get that child's education inputs. This is the one goal type where "unconfigured = omitted from base card too" is true, unlike the other 4.

**`WEALTH_FORMULA_GOALS_FAMILY.goals[]` payload shape change:** `YEAR_ONE` entries now carry an extra `beneficiary_profile_id` field (and probably a `beneficiary_name` for display, resolved via the same `profileServiceClient.listProfiles` call already made elsewhere in this engine) so the Dashboard can render "Year One — [child name]" per row instead of one unlabeled entry. `achieved_count`/`total_count` are no longer fixed at `N`/`5` — see the note above. Any frontend code reading this payload assuming exactly 5 entries or a fixed `total_count` must be checked before implementation (flagged below, not yet verified against `Dashboard.js`).

**Hard constraint honored — no real household data anywhere in schema/code (unchanged from v1):** migrations only create empty tables. Every objective, baseline figure, milestone target, rule, trigger event, education input, and insurance policy is admin-entered at runtime via the endpoints above — same pattern `policy_settings` already established.

**Rejected alternative — fold into `computeFormulaGoals()`/`policy_settings` in place (unchanged from v1):** rejected — entangles two data lifecycles, wrong domain for `policy_settings`. Note this is a different question from "should the 5 formulas themselves be corrected in place" (yes, decided above) — the additive `goal_plan`/`insurance_policy` tables and the bugfix to `computeFormulaGoals()`'s math are two independent decisions that happen to land in the same ADR revision.

**Rejected alternative — single wide `detail` JSONB with no structured milestone/rule tables at all (unchanged from v1):** rejected — milestones/rules need to stay queryable/summarizable across goals.

**Rejected alternative — evaluate `trigger_condition` as real system-enforced logic (unchanged from v1):** rejected — this is Epic 8 UC 8.3, deliberately still unbuilt.

**Rejected alternative — blend `insurance_policy` rows into a single "WITH insurance" number inside `computeFormulaGoals()`/`INSURANCE_FREE`'s live math:** rejected for this revision. `payout_structure` is deliberately heterogeneous JSONB (lump sum vs escalating monthly income vs sum-assured-at-maturity) — collapsing 3 incompatible shapes into one comparable currency figure needs real actuarial-style interpretation logic (e.g. what's the NPV of an escalating income stream vs a lump sum, over what horizon), which is a real feature in its own right, not a formula tweak. `INSURANCE_FREE`'s live `current_value`/`target_value` stay policy-free (buffer ÷ debt+fees+buffer, "what if we had zero insurance" framing) per the product owner's own "zero-dependency target" language; `insurance_policy` rows surface only in `computeGoalDetail()`'s richer payload as a raw list for now. Flagged below as a real gap if the product owner actually wants one blended number later.

**Rejected alternative — plain `UNIQUE(admin_id, goal_type, beneficiary_profile_id)` as the product owner literally described it:** rejected — see the Postgres NULL-uniqueness note above; it does not enforce the singleton invariant it was meant to enforce for the 4 non-`YEAR_ONE` goal types. `NULLS NOT DISTINCT` used instead.

**Impact:** Two new Flyway migrations — `application/flyway/wealth/V4__goal_plan.sql` (4 tables, additive) and `application/flyway/wealth/V5__insurance_policy.sql` (1 table, additive). New domain entities/ports/adapters in the wealth module (`GoalPlan`, `GoalMilestone`, `GoalRule`, `GoalTriggerEvent`, `InsurancePolicy` + matching use cases/repositories/resources). **`ProjectionCalculationEngine.computeFormulaGoals()` is modified in place** — all 5 formulas corrected, achieved-direction becomes an explicit lookup, new `THIRTY_SEVENTY_TARGET` transaction-aggregation logic, new `YEAR_ONE` per-child loop with a new `listGoalPlans()` dependency, new `FREEDOM_RUNWAY` core-runway-capital aggregation (PPF + CHILD-relation exclusions). `ExpenseCategory` enum widens from 5 to 8 values (domain + contract). `policy_settings` gains 2 keys (`insurance_free_legal_fees`, `insurance_free_academic_buffer`), 3 keys go dead (no removal needed — JSONB). New `SnapshotKey.WEALTH_GOAL_DETAIL_FAMILY` constant and `computeGoalDetail()` step, wired into `refreshAll()` after `computeFormulaGoals()`. Contract changes to `wealth.yaml` + its web-gateway mirror + `gateway.yaml` (additive paths/schemas for `goal-plans`/`insurance-policies`; `WEALTH_FORMULA_GOALS_FAMILY`'s documented payload shape changes for `YEAR_ONE` entries and `total_count`, which is a real, non-additive contract-level change to an existing, live snapshot payload — document it as such in `wealth.yaml`'s description, even though the transport (`JsonNode` passthrough) doesn't force a schema version bump). Not yet implemented — this ADR records the design only.

### Still underspecified or risky — flagged, not silently accepted

1. **`THIRTY_SEVENTY_TARGET`'s per-account transaction fetch is an N+1-shaped loop with no category filter at the query layer.** Every account of every household member gets a `listTransactions(from, to, txnType)` call for the trailing 3-month window, then category filtering happens in gateway memory (no `category` query param exists). Same pattern as `computeLiquidityTiers`/`computeEmiTracking` today, so not a new *kind* of risk, but this is the first `_FAMILY` step to page through full transaction history rather than account-level balances — could be materially slower for a household with years of transaction history and many accounts. Worth a perf check once built; not blocking the design.
2. **RESOLVED 2026-07-11 — `DEBT_CROSSOVER`'s denominator also excludes CHILD-relation loans**, symmetric with the numerator. `WEALTH_EMI_TRACKING_FAMILY.total_outstanding_balance` cannot be reused as-is (it's family-wide, no relation filter) — `computeFormulaGoals()`'s `DEBT_CROSSOVER` step needs its own relation-filtered sum over loan-type accounts, mirroring the numerator's SELF/SPOUSE-only filter. Currently no household member has a loan in a child's name, so this has no observable effect today, but the formula must filter correctly regardless.
3. **RESOLVED 2026-07-11 — `YEAR_ONE` shows a 0%-funded row for a child with zero `MUTUAL_FUND` accounts**, not a silent omission. Every CHILD-relation profile with a configured `goal_plan` row (`goal_type=YEAR_ONE`, `beneficiary_profile_id=<child>`) gets an entry in `WEALTH_FORMULA_GOALS_FAMILY.goals[]`, `current_value = 0` if no MUTUAL_FUND accounts exist yet — a deliberate nudge, not an edge case to hide. Only a child with no `goal_plan` row configured at all is omitted (nothing to compute against).
4. **RESOLVED 2026-07-11 — `insurance_policy` → `INSURANCE_FREE` "WITH insurance" comparison stays a raw list**, no blended total. Each policy renders as its own line (provider, premium, coverage/payout structure); no attempt to combine a lump sum with a 10-year escalating income stream into one number. Real actuarial blending is out of scope for this feature.
5. **`WEALTH_FORMULA_GOALS_FAMILY.total_count` no longer being a fixed `5` is a live payload shape change** — `Dashboard.js`'s Household Goals card (N/M achieved) needs to be checked against this assumption before implementation; not verified in this design pass.
6. **`goal_plan.education_base_cost`/`education_inflation_rate`/`education_years_to_entry` as 3 more always-nullable columns, meaningful only to one goal type, continues a pattern (`assumed_growth_rate` already does this) that doesn't scale well** — a 6th goal type with its own bespoke numeric inputs would mean more dead columns on every other goal type's row. Fine at this size (1 precedent-setting column becomes 4), but if a 6th goal type shows up with its own inputs, revisit whether goal-type-specific numeric inputs belong in `detail` JSONB instead (readable but unqueryable) or a per-goal-type child table (more tables, but no dead columns). Not a blocker now — flagged for whoever adds goal type 6.
7. **`FREEDOM_RUNWAY`'s corrected core-runway-capital aggregation is new code, not a reuse of `WEALTH_LIQUIDITY_TIERS_FAMILY` as v1 implied.** Confirm during implementation that this doesn't quietly diverge further from what the Liquidity Tier dashboard card shows (the two are now different aggregations over overlapping data — same account rows, different exclusion filters) — worth a short code comment cross-referencing both so a future edit to one doesn't silently desync the other.

---

## ADR-023: Application Console — Process Control from `web-gateway` + Per-Domain `error_log` Tables

**Status:** Accepted — **Implemented 2026-07-13** (Phase 4 of the platform-improvements plan). See "Implementation note" at the end of this ADR for two deliberate deviations from the design sketch below (column naming, `ServiceControlService` testing approach) and the open questions' resolutions.

**Decision:** The admin-only Application Console (live service status, start/stop controls, per-service error stats, error log entry viewer) introduces two genuinely new architectural surfaces, both scoped, deliberate exceptions to existing rules:

1. **Process control from `web-gateway`.** A new `com.suchika.gateway.console` package, containing `ConsoleResource` (REST endpoints), `ServiceControlService` (starts/stops individual services by shelling out to `scripts/run-local.ps1`/`.sh` and `scripts/stop-local.ps1`/`.sh`), and `ServiceStatusService` (polls each service's real `/q/health` and reads the PID registry at `~/.suchika/run/<service>.pid` to report live status).
2. **Per-domain `error_log` tables.** Each of the four domains gets its own `error_log` table (own schema, own Flyway migration), exposing `GET /v1/errors?since=&limit=`. `web-gateway` adds `GET /v1/console/errors`, fanning out over REST to aggregate across all four domains.

Both surfaces are gated behind a single config flag, **`suchika.console.enabled=false` by default**, in all five services' `application.properties`. When `false`, `ConsoleResource`'s endpoints return 404/disabled and `ServiceControlService`/`ServiceStatusService` are never invoked — the feature cannot ship live by accident.

### Part 1 — Process control from `web-gateway`

**Context:** ADR-002 establishes `web-gateway` as a BFF with no DB of its own, composing domain REST calls. ADR-013 extended this to computation (`ProjectionCalculationEngine`) but kept the gateway read-only and side-effect-free against the outside world — every existing gateway responsibility (REST aggregation, CQRS projection compute) stays inside the process, touching only Postgres (`projections` schema) or outbound HTTP to domain services. No service in this codebase has ever executed an OS process. This ADR is the first.

**Why `web-gateway` is the right home for it, not a new sixth service:** the Console is inherently cross-service — it needs to see and control all five services (profile/wealth/health/household/gateway) plus the frontend from one page. `web-gateway` already aggregates cross-domain data for the frontend and is the only service with no domain data of its own to protect from an unrelated new concern. A new dedicated "ops service" would be a sixth Quarkus service, its own port, its own startup-order entry, for a feature that is itself about managing the other five — disproportionate for what Phase 4 needs.

**`ServiceControlService` invokes the scripts in per-service form, not whole-stack.** `run-local.ps1`/`.sh` and `stop-local.ps1`/`.sh` today only support "start/stop everything." A parallel workstream is adding a `-Service <name>` parameter to both scripts so they can target one named service (`profile`, `wealth`, `health`, `household`, `gateway`, `web` — the same `name` values already defined in `scripts/services.json`). `ServiceControlService` calls the scripts with this new per-service argument (e.g. `run-local.ps1 -Service wealth`), never the whole-stack form — the Console's UI controls one service at a time, and invoking the whole-stack form from a single-service button would restart services the admin didn't ask to touch.

**Why this is an acceptable, scoped exception to the gateway's established role (justified the same way ADR-021 justified its own deliberate exception):**
- `README.md` states the app is "owned and run locally" — one deployment, one household, one admin. `ADR-021` records the product owner's own framing, stated twice: he is the only person who ever logs into the app.
- `ADR-005` defers all real identity/auth (OIDC/OAuth2, RBAC) to v1.0 and is still unimplemented — there is no real authenticated-admin boundary to place this behind today beyond the frontend's existing role-gated routing (`<ProtectedRoute requiredRole="admin">`).
- Given both of the above, a feature that lets "the one person who can already reach this app" restart "the services that same person already starts and stops manually via `dp`/`dw`/`rl`/`sl` today" is not introducing a new privilege boundary — it is giving the existing sole admin a UI for an action they can already perform from a terminal on the same machine. It is not safe in a multi-user or remotely-hosted deployment, which is exactly why it must be flag-gated off by default (below) rather than assumed safe by design.
- This mirrors ADR-021's shape of argument precisely: a pragmatic, scoped exception justified by *this app's actual current shape* (local, single-admin, no real auth), not a speculative claim that process control is safe in general. Same as ADR-021, it is deliberately cheap to gate/remove once v1.0 auth lands.

**Config flag — mandatory, default off:** `suchika.console.enabled=false` in every service's `application.properties` (profile/wealth/health/household expose `error_log` reads under the same flag; `web-gateway` gates `ConsoleResource` entirely). This is not a soft feature toggle for UX convenience — it is the load-bearing control that keeps process-control and cross-service error visibility from ever being live in an environment where the "local, single-admin" assumption above doesn't hold (a shared server, a demo deployment, a codespace someone forgot to lock down). Flipping it to `true` is a deliberate per-environment decision, never a shipped default.

**Re-examination trigger — explicit:** this whole surface must be re-examined once real OIDC auth lands at v1.0 (ADR-005). At that point, "the one person who can reach this app" is no longer a safe proxy for "the admin" — process control and error-log visibility need to sit behind a real authenticated-admin check server-side (not just the frontend's `<ProtectedRoute>`, which is a routing convenience, not a security boundary), and the default-off config flag's justification (documented above) needs to be revisited rather than assumed to still hold.

**Rejected alternative — a sixth dedicated "ops" Quarkus service:** rejected as disproportionate — see above. Also would need its own DB-less-BFF-style justification duplicating ADR-002's reasoning for a feature whose whole purpose is managing the other five services, not adding a sixth domain.

**Rejected alternative — no config flag, rely on the frontend's `<ProtectedRoute requiredRole="admin">` alone:** rejected — that guard is client-side routing, not a server-side control. Without a backend flag, anyone who can reach the gateway's REST port directly (bypassing the frontend entirely) could hit `ConsoleResource` regardless of frontend role checks. The flag is the only real gate until v1.0 auth exists.

### Part 2 — Per-domain `error_log` tables, gateway aggregation

**Context:** the only existing error-log precedent is `wealth.upload_error_log` (ADR-014), scoped narrowly to CSV parsing — persisted via `CsvParseException`'s persist-then-rethrow pattern, with no `profile_id` column of its own (it inherits scope transitively through `upload_id → account_id → profile_id`, per ADR-020's Q33 child-table rule: a table unambiguously owned by an already-scoped parent doesn't get its own copy). No cross-domain error table exists today, and no domain besides wealth logs structured errors to the DB at all.

**Decision — one `error_log` table per domain, not one shared table:** each of profile/wealth/health/household gets its own `error_log` table, in its own schema, via its own Flyway migration (e.g. `application/flyway/wealth/V6__error_log.sql`, following whichever version number is next uncommitted in each domain's chain). Each domain exposes `GET /v1/errors?since=&limit=`, matching the existing `GET /uploads/{id}/errors` shape (paginated, newest-relevant-window query, no full-table dump). `web-gateway`'s new `GET /v1/console/errors` fans out over REST to all four domains and merges the results — this is the exact aggregation-not-joins pattern ADR-013 already established for every other gateway aggregation endpoint (dashboard snapshots, Action Center).

**Why one table per domain, not a shared cross-domain table:** a single shared `error_log` table (e.g. living in a new `console` or `projections` schema, written to by all five services) would require either (a) each domain writing cross-schema, which no domain does anywhere else in this codebase, or (b) the gateway writing it on domains' behalf, which means the gateway now owns write-path data integrity for errors it didn't generate and doesn't fully understand the shape of. Either way, a shared table becomes the exact structure ADR-003 (no cross-domain DB joins/shared tables) and ADR-006 (profile-scoped isolation, enforced per-domain in the adapter layer) were written to prevent — one more service reading or writing another domain's error data outside of REST is precisely the coupling those two ADRs rule out. Keeping `error_log` per-domain means each domain's adapter layer owns writing and reading its own error rows exactly like every other table it owns, and the gateway's role stays "aggregate via REST," identical to its role for every other cross-domain view it already builds.

**Schema — per domain, own schema, own migration (sketch, `V<next>__error_log.sql` in each of `profile/`, `wealth/`, `health/`, `household/`):**

```sql
CREATE TABLE <domain>.error_log (
    id           UUID         NOT NULL DEFAULT gen_random_uuid(),
    error_type   VARCHAR(50)  NOT NULL,
    message      TEXT         NOT NULL,
    stack_trace  TEXT,
    occurred_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_<domain>_error_log PRIMARY KEY (id)
);
CREATE INDEX idx_<domain>_error_log_occurred_at ON <domain>.error_log(occurred_at);
```

No `CHECK` constraints (ADR-020) — `error_type` is a plain `VARCHAR`, soft-validated at the contract layer (ADR-010), not a SQL enum. No `profile_id`/`admin_id` column — see scoping note below.

**Scoping — deliberately not `profile_id`- or `admin_id`-scoped, unlike every other domain table (flagged, not silently assumed):** every existing domain table carries either a direct `profile_id` (ADR-006) or, for household-level config, an `admin_id` (ADR-022's `goal_plan`/`insurance_policy`/`policy_settings` precedent). `error_log` rows are different in kind — they capture operational/system failures (an unhandled exception, a failed HTTP call, a bad CSV row already covered by `upload_error_log`), not member-attributable household data. There is no natural "which household member does this stack trace belong to" answer for most of them. Given the single-admin, single-household model this app runs under today (ADR-017, ADR-021), and that the Console is admin-only, this ADR proposes `error_log` stays unscoped — visible to the one admin in full, with no per-profile filtering. **This is flagged as worth explicit product-owner confirmation, not asserted as obviously correct** — it is a new scoping shape (neither `profile_id`- nor `admin_id`-keyed) that no existing table uses, and deserves the same explicit sign-off ADR-006/ADR-017/ADR-022's scoping decisions each got, rather than being inferred here by analogy alone.

**Rejected alternative — one shared cross-domain `error_log` table:** rejected — see above, violates ADR-003 and ADR-006's per-domain-adapter-owns-its-own-queries model.

**Rejected alternative — extend `wealth.upload_error_log`'s shape/scope to be the one general-purpose error table, reused by all domains:** rejected for the same reason as the shared-table alternative, plus `upload_error_log` is intentionally narrow to CSV parsing (ADR-014) — its schema (`upload_id`, `missing_columns`) doesn't generalize to, say, a health-domain HTTP 500. Widening it would tangle a narrow, working, tested error type with a general one.

**Rejected alternative — no persistence at all, Console reads only in-memory/log-file tail (`lnav`-style) instead of a DB table:** rejected for this design — the Console's stated requirement is "per-service error statistics," which needs queryable, aggregable structure (`since`/`limit`, counts over a window) that grepping rotating log files doesn't cleanly provide. Log-file tailing (`lnav-dev`/`logs`) already exists as a separate, complementary tool for raw log inspection — this ADR doesn't replace it, it adds structured error aggregation on top.

**Impact:** four new Flyway migrations (one per domain, additive, no changes to existing tables). Four new `GET /v1/errors` endpoints (domain-owned, following each domain's existing `ports`/`adapters` pattern). New `com.suchika.gateway.console` package in `web-gateway`: `ConsoleResource`, `ServiceControlService`, `ServiceStatusService`. New `suchika.console.enabled` config property in all five `application.properties`, default `false`. Contract additions to each domain's `{domain}.yaml` (`/errors` path) and to `gateway.yaml` (`/v1/console/errors`, `/v1/console/services`, `/v1/console/services/{name}/start`, `/v1/console/services/{name}/stop` or equivalent — exact paths are an implementation detail for the backend engineer, not fixed by this ADR). Not yet implemented — this ADR records the design only.

### Testability and quality gates

- `ConsoleResource`/`ServiceControlService`/`ServiceStatusService` live in `web-gateway`'s `adapters`-equivalent layer (the gateway has no `domain`/`ports` split today per ADR-002/013 — it is a thin aggregation/compute layer, not a hexagonal domain). `ServiceControlService` wraps `ProcessBuilder` invocation behind an interface so it can be unit-tested with a fake process runner (no real `ProcessBuilder` execution in unit tests); `ServiceStatusService`'s HTTP polling and PID-file reads are tested the same way the existing `WealthServiceClient`/`ProfileServiceClient` REST calls are tested today — `@InjectMock @RestClient` (ADR-011) for the `/q/health` polling, a fake filesystem root for PID-file reads.
- Each domain's `error_log` write path (wherever an unhandled exception is caught and persisted) and `GET /v1/errors` read path get the same test coverage every other adapter endpoint gets — domain-layer unit tests with plain `new`, adapter-layer tests against the shared local Postgres `%integration-test` profile (Testcontainers is documented but not yet adopted anywhere in this codebase, Q34/Q35 — do not assume it here either).
- `suchika.console.enabled=false` should have its own test asserting `ConsoleResource` returns 404/disabled when the flag is off, so the default-off guarantee is enforced by CI, not just by convention.
- No new ArchUnit exclusion is anticipated: `ServiceControlService`/`ServiceStatusService` live in `adapters`-equivalent gateway code, which already may use `@Inject`/HTTP types; nothing here touches `domain/` in any of the four real domains, so the existing `DomainRulesTest` rules apply unchanged.

### Open questions / flagged for product owner

1. **Should `error_log` carry any scoping column at all** (`profile_id`, `admin_id`, or genuinely none)? This ADR proposes none, for the reasons above, but it's a new shape with no direct precedent — worth explicit sign-off before the first migration is written, the same way ADR-006/017/022's scoping calls were each explicitly confirmed rather than inferred.
2. **`suchika.console.enabled` re-examination at v1.0** is asserted here as mandatory but not scheduled anywhere yet — recommend adding an explicit v1.0 checklist item in `ROADMAP.md` alongside ADR-005/ADR-007's existing "deferred to v1.0" items, so it isn't quietly forgotten once real OIDC auth ships.
3. **Retention/growth of `error_log` tables** is unspecified — no TTL, no row cap, no archival policy in this design. For a locally-run personal app this is probably fine at current scale, but flagged rather than silently assumed, consistent with how ADR-022 flagged its own unresolved scale questions rather than deciding them unilaterally.

### Implementation note — 2026-07-13

Built as designed above, with two deliberate deviations from the sketch and a resolution for open question 1:

- **Column naming differs from the sketch's `error_type`/`stack_trace`/`occurred_at`.** Implemented as `error_code VARCHAR(50)`, `http_status INT`, `message VARCHAR(500)`, `details VARCHAR(1000)` (nullable), `created_at TIMESTAMPTZ` instead — chosen because these map directly to what `ApplicationException` (the thing actually being persisted, via the new `ErrorLogRecorder` port called from `ApplicationExceptionMapper`) already carries (`getErrorCode()`, `getStatusCode()`, `getMessage()`, `getDetails()`), and because no table in this codebase prefixes its PK/index names with the schema/domain (`wealth.account`'s constraint is `pk_account`, not `pk_wealth_account`) — `pk_error_log`/`idx_error_log_created_at` (unqualified) matches that existing convention rather than the ADR sketch's `pk_<domain>_error_log`. No stack trace is stored — `ApplicationException` doesn't carry one distinct from the Java exception's own, and the existing `AppLogger.error(message, throwable)` call already logs it to the file/console log for that purpose.
- **Open question 1 (scoping) resolved as proposed: no `profile_id`/`admin_id` column.** `error_log` stays fully unscoped, matching the ADR's proposed default. Confirmed consistent with `wealth.upload_error_log`'s existing precedent (also unscoped at the table level).
- **`ServiceControlService` does not wrap `ProcessBuilder` behind a fakeable interface**, unlike the Testability section's suggestion. Instead: the unknown-service validation path (`BadRequestException` before any process is touched) is unit-tested directly (`ServiceControlServiceTest`), and the full start/stop wiring through `ConsoleResource` is tested by `@InjectMock`-ing the whole `ServiceControlService` bean at the resource boundary (`ConsoleResourceTest`) rather than at a process-runner seam beneath it. The real script-invocation path was instead verified by manually running `run-local.ps1 -Service wealth` / `stop-local.ps1 -Service wealth` (and the bash equivalents) against the live local stack. Judged an acceptable, simpler tradeoff given this feature's scope; revisit with a real fake-process-runner abstraction if `ServiceControlService` grows non-trivial branching logic.
- **`suchika.console.enabled=false` default-off behavior has its own dedicated test** (`ConsoleResourceDisabledTest`, deliberately using the plain default `@QuarkusTest` profile with no config override) asserting every `ConsoleResource` endpoint 404s, addressing the Testability section's third bullet directly.
- Open questions 2 (ROADMAP v1.0 re-examination checklist entry) and 3 (retention policy) are **not resolved by this implementation pass** — still open for product-owner decision, as the ADR itself flagged.

### Frontend implementation note — 2026-07-13

The admin-only Console page (`web/src/pages/Admin/ApplicationConsole.js`, route `/admin/console`) was built after the backend above, consuming it exactly as designed — one deviation and one real bug found during live verification:

- **Does not import `web/src/api/generated.ts` at runtime**, despite the contract/OpenAPI types already existing there. Every other page in this codebase calls hand-written `fetch()` wrappers in `api/<domain>.js`, not the generated client (a pre-existing, repo-wide gap between `FRONTEND_GUIDELINES.md`'s documented standard and actual practice — out of scope to fix here). A new `web/src/api/console.js` wrapper was added following that same established convention; `generated.ts` was read only as a reference for exact field names/shapes, not wired in as a consumer.
- **Real bug caught live, not just by unit tests:** `ConsoleErrorAggregationService`'s class-level javadoc (Part 2 above) says a down domain "contributes an empty array," but the code's `fetch()` catch block actually returns a one-element array shaped `{"error": "..."}` — not the `ErrorLogResponse` shape the contract documents. The frontend's error-panel rendering was fixed to handle both shapes gracefully (falls back to `entry.error` for the message, labels it `SERVICE_UNREACHABLE`) after this was found by driving the real page against the real gateway with all four domains stopped, not by trusting the documented shape. The javadoc/code mismatch itself was not corrected here (frontend-only task) — worth a small follow-up fix in `ConsoleErrorAggregationService`.
