# Architecture Review — 2026-07

| | |
|---|---|
| **Type** | Reference — Point-in-time Review |
| **Audience** | Architect, product owner, all developers |
| **Status** | Active |
| **Last updated** | 2026-07-03 |

## Objective

Project-wide architectural health check at v0.6 completion. Covers: hexagonal layering compliance, technical debt hotspots, documentation/contract drift, and improvement recommendations. Complements (does not replace) the existing Architect/BA/QA review sections already appended to `ROADMAP.md` (2026-06-29) — this review is scoped to structure and drift, not feature delivery status.

---

## 1. Architectural Health — Hexagonal Layering

**Verdict: compliant.** `shared/src/test/java/com/suchika/architecture/DomainRulesTest.java` enforces the full rule set on every `./gradlew test` run:

| Rule group | What it checks | Status |
|---|---|---|
| Domain purity (persistence) | No `jakarta.persistence.*`, `org.hibernate.*`, `io.quarkus.panache.*` in `..domain..` | Enforced |
| Domain purity (HTTP/CDI) | No `jakarta.ws.rs.*`, `jakarta.inject.*`, `io.quarkus.arc.*` in `..domain..` | Enforced |
| Domain isolation from ports/adapters | `..domain..` must not import `..ports..` or `..adapters..` | Enforced |
| Ports must not depend on adapters | `..ports..` → `..adapters..` forbidden | Enforced |
| Input/output ports must be interfaces | `..ports.input..` / `..ports.output..` classes must be interfaces | Enforced |
| JPA entities confined to adapters | `@Entity`-annotated classes must live in `..adapters..` | Enforced |
| Cross-domain isolation | wealth/health/household/profile must not import each other | Enforced (4 symmetric rules) |
| `shared/` leaf-module rule | `shared` must not import any domain package | Enforced |
| Logging discipline | No raw SLF4J/JUL outside `shared` | Enforced |
| Gateway resource test coverage | Every `..gateway..*Resource` class needs a `*Test`/`*IT` | Enforced (added v0.5/v0.6) |
| Ports.input test coverage | Every `ports.input` interface must be referenced by a test class | Enforced (added v0.6 — this is the rule that immediately surfaced 3 zero-coverage resources) |

This is a mature, comprehensive ArchUnit suite for the project's size. The v0.6 addition (`ports_input_interfaces_must_be_referenced_by_a_test_class`) is a good example of the intended pattern — a structural rule that actively found real gaps (`AdminResource`, `ProfileResource`, `PhysicalAssetResource`) rather than a passive checkbox.

**Known, accepted deviation:** ADR-019 documents `profileId` as a plain field on 7 domain entities across wealth/health/household — a deliberate, ADR-recorded departure from ADR-006's stricter wording. This is correctly handled: documented, not silently drifted, and explicitly not flagged by any new ArchUnit rule (Option B was considered and rejected). No action needed.

**No violations found** in the current codebase for any of the 11 rule groups as of the 2026-07-03 commit (`68fac24`).

---

## 2. Technical Debt Hotspots

Ranked by risk, cross-referencing the existing `ROADMAP.md` Architect/BA reviews (2026-06-29) to avoid duplicating already-tracked items — only new or re-confirmed items are detailed here.

### 2.1 Contract/mirror drift (see Section 3 for full detail)
- `application/web-gateway/src/main/resources/gateway.yaml` is stale relative to `application/contract/gateway.yaml` — missing 6 of 10 tag sections (Physical Assets, Calendar Events, Inventory Items, Goals, Projections, Vacation Planner) with **zero paths**. Confirmed **not** read by any build config (`web/package.json`'s `generate:api` points at the canonical file directly; no Gradle resource reference found in `web-gateway`). This makes it dead documentation weight, not a functional break — but it actively misleads any developer who greps it expecting an accurate spec.
- `application/web-gateway/src/main/resources/profile.yaml` mirror was stale until this session partially closed the gap (per `documents/domain-state/profile.md`) — still confirmed missing `PATCH /v1/admins/{admin_id}/policy` relative to canonical (see Section 3.2 for line-level detail).

### 2.2 Domain-state documentation vs. actual Flyway schema drift (new finding this session)
Every domain's `documents/domain-state/<domain>.md` "Database Schema" table describes a **simplified/paraphrased** version of the real schema, and in the wealth case, the paraphrase is materially wrong:

- `documents/domain-state/wealth.md` lists `wealth.account` columns as `id, profile_id, name, type, balance, is_active, metadata`. The actual V1+V4+V6 schema has `id, profile_id, institution_name, account_name, account_type, currency, is_active, created_at, opening_balance, credit_limit, interest_rate, emi_amount, metadata` — there is no `name`/`type`/`balance` column at all (`account_name`/`account_type`/`opening_balance` are the real names). Anyone reading only the domain-state file (not the migration) would query the wrong column names.
- Same file lists account types as `SAVINGS, CURRENT, CREDIT_CARD, HOME_LOAN, PERSONAL_LOAN, INVESTMENT, FD` (this matches the OpenAPI contract / current application behavior) but the original V1 CHECK constraint (now dropped in V4) had listed `SAVINGS, CURRENT, CREDIT_CARD, LOAN, MUTUAL_FUND, FIXED_DEPOSIT` — an intentional pre-V4 value set, since superseded. Not a live bug (the CHECK is dropped, contract enum is authoritative) but worth noting: the domain-state schema table is describing the *contract's* enum, not any DB artifact, and doesn't say so.
- `documents/domain-state/wealth.md` lists `wealth.transaction` as having its own `profile_id UUID FK` column. The actual V1 migration has **no `profile_id` column on `wealth.transaction` at all** — it is resolved transitively via `account_id → wealth.account.profile_id` (this is exactly the ADR-019/Q33 trade-off). This is the most consequential drift found: the domain-state doc materially overstates what's actually in the table, in a way directly relevant to the Flyway consolidation plan's Q33 decision (see the companion plan document).
- `documents/domain-state/profile.md` describes only 6 relation values (`SELF, SPOUSE, CHILD, PARENT, SIBLING, OTHER`) — the actual V1 migration's original CHECK (dropped in V2) and code comments describe 9 (`SELF, SPOUSE, CHILD, PARENT, PARENT_IN_LAW, SIBLING, GRANDPARENT, GRANDCHILD, OTHER`). Need to confirm which set the current `application/contract/profile.yaml` OpenAPI enum actually uses — if it's 9, `domain-state/profile.md` undercounts; if it's 6, the V1 SQL comment is aspirational/stale. Flagged as a genuine open question below (Q46).
- `documents/domain-state/health.md` and `documents/domain-state/household.md` schema tables are reasonably accurate against their respective Flyway files — no material drift found there.

**Root cause:** domain-state files are hand-maintained summaries, not generated from the schema. They drift the same way any hand-written doc drifts from code — confirmed by the two real contract-drift cases already found and fixed this session in the profile domain. This is a pattern, not a one-off.

**Recommendation:** either (a) treat domain-state "Database Schema" tables as intentionally simplified summaries and add a one-line disclaimer + link to the actual Flyway file for the authoritative shape, or (b) run `/sync-context` (the existing skill) on a fixed cadence (e.g., every version bump) to regenerate these tables from real migration files rather than hand-editing them. Option (b) is stronger and the tooling already exists.

### 2.3 Previously tracked, still open (from `ROADMAP.md` 2026-06-29 review — not re-litigated in depth here)
- No ArchUnit rule verifies `profile_id` presence in adapter query predicates (MEDIUM, v1.0 item, `ROADMAP.md` item 7).
- API versioning strategy (PROP-004) still unresolved — all endpoints under `/v1/`, becomes urgent once auth ships (`ROADMAP.md` item 8).
- `ProjectionCalculationEngine.refreshAll()` synchronous, all-or-nothing per step (mitigated by per-step try/catch since v0.4/Bug 3, but still fully synchronous end-to-end) — `ROADMAP.md` item 10, deferred to v1.0.
- Contract tests (OpenAPI schema validation of live responses) — resolved as Q11 (Option A, Atlassian Swagger Request Validator), targeted at v0.6, but v0.6's actual delivered scope (see `ROADMAP.md` "v0.6 Scope (revised)") does not list this as done — carry forward, confirm status before v1.0.

### 2.4 New observation: Flyway migration count and enum-CHECK churn
Every domain has at least one dedicated "remove enum constraints" migration (`wealth/V5`, `health/V2`, `household/V3`) that exists solely to undo an earlier CHECK constraint the same team added in `V1`. This is not a bug — ADR-010 is explicit that this is acceptable churn — but it is a recurring pattern (3 domains independently made and then corrected the same mistake) that suggests the "no CHECK on discriminators" rule was not obvious enough at initial authoring time, only caught on review. Worth reinforcing in onboarding/agent prompts (already flagged similarly in `ROADMAP.md` item 5 for the `upload_error_log.error_type` case specifically — this generalizes it to 3 domains, not 1).

---

## 3. Documentation / Contract Drift — Detailed Findings

### 3.1 Confirmed clean: wealth, health, household mirrors
```
application/contract/wealth.yaml      <-> application/web-gateway/.../wealth.yaml      : IDENTICAL (1389 lines each)
application/contract/health.yaml      <-> application/web-gateway/.../health.yaml      : IDENTICAL (578 lines each)
application/contract/household.yaml   <-> application/web-gateway/.../household.yaml   : IDENTICAL (914 lines each)
```
No drift. These three domain contract mirrors are byte-identical to their canonical source. The pattern that broke for `profile.yaml` (see below) did not repeat here — likely because Epic 8's wealth/household changes were shipped with contract sync as an explicit step each time, while profile's `/admins` policy endpoint was added later, out of band.

### 3.2 Confirmed stale: profile.yaml mirror
```
application/contract/profile.yaml                                  : 400 lines, 5 paths
application/web-gateway/src/main/resources/profile.yaml             : 559 lines, 4 paths
```
The mirror is **missing** `PATCH /v1/admins/{admin_id}/policy` (present in canonical, absent in mirror) and uses a different path-param name for the profile-by-id route (`{profile_id}` in the mirror vs `{id}` in canonical — cosmetic if the Rest Client still binds correctly, but another sign of independent, non-synced edits). This is a live functional risk: `WealthGatewayResource`-style proxies use the mirror file as the MicroProfile Rest Client's OpenAPI-derived interface source; if `ProfileServiceClient`'s Java interface was hand-written independently of this mirror (which domain-state notes suggest — the policy proxy route was described as "previously missing, silently 500ing through the gateway" and was fixed directly in Java, not by regenerating from the mirror), the mirror is now purely decorative and no longer trustworthy as a spec. Recommend either deleting stale per-domain mirrors that no longer round-trip to codegen, or wiring an automated diff check into CI (`quality-manager`/`sonar` skill candidate).

### 3.3 Confirmed stale: gateway.yaml mirror (repeats the pattern found this session in profile)
```
application/contract/gateway.yaml                                   : 2280 lines, 10 tag sections
application/web-gateway/src/main/resources/gateway.yaml              : 629 lines, 4 tag sections
```
Canonical `gateway.yaml` tags: Household Profiles, Accounts, Transactions, StatementUploads, **Physical Assets, Calendar Events, Inventory Items, Goals, Projections, Vacation Planner**.
Mirror `gateway.yaml` tags: Household Profiles, Accounts, Transactions, StatementUploads only — **6 tag sections entirely absent, zero paths for any of them.**

This is the same class of drift already found and fixed this session for the profile domain (per the instruction preamble) — confirmed here to also affect the gateway's own self-mirror, and at a larger scale (6 missing tag sections vs. profile's 1 missing path). Traced the actual consumer: `web/package.json`'s `generate:api` script reads `application/contract/gateway.yaml` directly (the canonical file, not this mirror), so **the frontend typed client is not at risk** — this mirror file appears to be an orphaned artifact with no active build-time consumer found in `web-gateway/build.gradle.kts` or `application.properties`. Recommend confirming with `devops`/`quarkus-developer` whether this file is genuinely dead (candidate for deletion) or whether some tool still reads it (e.g. a manual Swagger UI static mount) before removing it — do not delete speculatively in this planning-only pass.

### 3.4 Domain-state vs. Flyway schema drift
See Section 2.2 above — the wealth `transaction.profile_id` and `account` column-name drift is the most material finding; carried forward into the companion Flyway consolidation plan's Phase 0 audit since it directly bears on Q33.

---

## 4. General Recommendations

1. **Adopt a drift-detection gate.** The profile.yaml and gateway.yaml mirror drift, and the wealth domain-state schema drift, are all instances of the same underlying problem: hand-maintained duplicate representations of a single source of truth silently diverge. A CI check (`diff` the canonical contract against each mirror; fail the build on mismatch) would have caught both contract-drift cases automatically instead of requiring a manual review pass. Low effort, high value — recommend as a `quality-manager`/CI backlog item for v0.6-close or early v1.0.

2. **Decide the fate of `web-gateway/src/main/resources/*.yaml` mirrors.** If they have no active consumer (as this review's grep found for `gateway.yaml`), stop maintaining them — delete and update `ARCHITECTURE_DECISIONS.md` ADR-009 to reflect that only domain contracts (used by MicroProfile Rest Client) need mirroring, not the gateway's own contract. If they do have a consumer not found by this pass, wire the sync into `npm run generate:api`'s backend equivalent (a Gradle task that copies canonical → mirror on every build) so they can never drift again.

3. **Re-baseline domain-state schema tables from Flyway, not memory.** Use the existing `/sync-context` skill to regenerate the "Database Schema" section of each `domain-state/<domain>.md` directly from the current `application/flyway/<domain>/*.sql` files rather than continuing to hand-edit summaries. This would have caught the wealth `account`/`transaction` column-name drift immediately.

4. **Formalize the "no CHECK on discriminators" rule earlier in the authoring flow**, given 3 independent domains made the same mistake and needed a corrective migration. Consider a lightweight ArchUnit-adjacent check (a script, not necessarily ArchUnit itself, since this is DDL not Java) that flags any new `CREATE TABLE`/`ALTER TABLE ADD CONSTRAINT ... CHECK (... IN (...))` pattern in a Flyway diff for manual review before merge — a linter, not a hard gate, since some CHECK-with-IN patterns are legitimate (e.g. `chk_admin_relation_is_self`-style structural rules aren't a list of interchangeable strings).

5. **Carry forward the still-open `ROADMAP.md` 2026-06-29 items** (profile_id ArchUnit rule, PROP-004 API versioning, contract-test delivery status) into v0.7/v1.0 planning — this review did not find reason to change their priority, only reconfirmed they remain open.

---

## Related Documents

- `documents/flyway-consolidation-plan.md` — the phased plan this review's Section 2.2/3.4 findings feed directly into (Phase 0 audit)
- `documents/OpenQuestions.md` — Q31-Q33 (existing), Q44-Q48 (new, if any raised — see companion plan)
- `documents/ARCHITECTURE_DECISIONS.md` — ADR-006, ADR-010, ADR-019 (all referenced above)
- `documents/ROADMAP.md` — Architect/BA review sections, 2026-06-29 (prior-review baseline, not superseded by this one)
