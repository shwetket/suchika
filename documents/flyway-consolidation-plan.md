# Flyway Consolidation Plan — Phased (Executed, Reconciled Against Final Resolutions)

| | |
|---|---|
| **Type** | Plan — Executed |
| **Audience** | Product owner, architect, all domain developers |
| **Status** | **Executed 2026-07-05.** All 5 domains' consolidated V1 scripts are live; `app_db` was fully reset and every domain's adapter test suite passes against it. See reconciliation note below — the shipped result differs from this plan's own draft recommendations in three places. |
| **Last updated** | 2026-07-05 (reconciliation note added; see ADR-020) |

## Reconciliation Note (2026-07-05) — read this before trusting any table below

This plan was drafted 2026-07-03 under a working assumption ("no FK, no CHECK, no UNIQUE at all"). The product owner's actual resolutions, logged 2026-07-04, **reversed three of this plan's draft positions**:

| Plan drafted (below) | Actually resolved (`OpenQuestions.md`) | Shipped state (verified 2026-07-05) |
|---|---|---|
| No FK constraints anywhere (Section 2) | **Q31: keep FK for referential integrity** | All 5 domains' consolidated V1 scripts have FK constraints restored (`fk_account_profile`, `fk_vital_profile`, `fk_event_profile`, `fk_snapshot_profile`, etc.) |
| `uq_transaction_dedup`/`uq_admin_email` dropped (Q46 in this doc) | **Q46: keep UNIQUE constraints** | Both UNIQUE constraints present in shipped SQL |
| "Entity name" columns → `VARCHAR(200)` (this plan's own Q47 resolution, Phase 4) | **Q44: VARCHAR(50)** for name columns project-wide | All name columns (`account_name`, `institution_name`, `asset_name`, `display_name`, `full_name`) are `VARCHAR(50)` |
| CHECK constraints dropped, Q45 mitigation "recommended, not yet verified" | **Q45: verify domain-layer enforcement before dropping** | Verified and added this session: `Transaction.create()` (amount≥0), `VitalReading.create()` (value_primary>0, BP-secondary-required), `DoctorVisit.create()` (to_date≥from_date, doctor_name-required), `GoalService` (current_amount≥0) |

**Do not use the Phase 1-5 before/after tables below as current truth for VARCHAR widths, FK presence, or UNIQUE presence** — they reflect the pre-resolution draft. For current schema truth, read the actual files in `application/flyway/{domain}/V1__init_*_consolidated.sql` or `documents/domain-state/<domain>.md`. This plan's tables remain useful for: the naming-inconsistency audit (Phase 0.2), the Q33 root-vs-child `profileId` rule (Phase 0.3, unaffected by the FK/UNIQUE reversal), and the phase ordering/verification methodology (still valid).

Full decision record: `documents/ARCHITECTURE_DECISIONS.md` ADR-020.

## Scope and Ground Rules (read first)

This plan is written under an explicit, current-session product-owner override of three normally-standing rules, already logged in `documents/OpenQuestions.md` as **Q31, Q32** (and this document's own **Q33** resolution below). Do not treat these overrides as precedent for any other work — they are scoped to this consolidation effort only.

1. **Migrations may be edited/replaced in place** (normally forbidden — `CLAUDE.md`: "never edit a committed migration"). Justification: dev phase, no production data yet, per-domain ephemeral local DBs only.
2. **One consolidated script per domain** — collapse each domain's `V1__…Vn__` chain into a single canonical `V1__` file. Old files are deleted/replaced, not appended to.
3. **No DB-level FK or CHECK constraints at all** (normally `CLAUDE.md` keeps NOT NULL/PK/FK/UNIQUE/business-rule CHECKs; only enum CHECKs are excluded). This plan goes further per instruction — see Section 2 for the accepted risk and the recommended mitigation.

**This plan does not execute anything.** No file under `application/flyway/` is modified. No migration runs. No live DB is touched. Every phase below ends with a verification step to run *after* a human decides to execute — not run by this planning pass.

---

## Phase 0 — Investigation / Audit Findings

Audited every file under `application/flyway/{profile,wealth,health,household,projections}/` plus `application/flyway/00_bootstrap.sql` and the test-seed files. Full results below; this is the factual basis for Phases 1-5.

### 0.1 Current migration inventory

| Domain | Files | What each does |
|---|---|---|
| profile | V1, V2, V3 | V1: `profile.profile` (single-admin model, 4 CHECK constraints). V2: adds `profile.admin`, `admin_id` FK, drops V1's CHECKs + `is_admin` column, adds `uq_admin_self_profile`. V3: adds `policy_settings JSONB` to `admin`. |
| wealth | V1–V7 | V1: `account`/`statement_upload`/`transaction` + enum CHECKs. V2: `physical_asset`. V3: `status` lifecycle + `upload_error_log`. V4: static financial columns on `account`, drops `chk_account_type`. V5: drops remaining enum CHECKs (txn_type, asset_type, registration_type, upload_status, error_type) + some indexes on small tables. V6: `account.metadata JSONB`. V7: `transaction.upload_id` made nullable. |
| health | V1, V2 | V1: `vital_reading` + `doctor_visit`, both NOT NULL `profile_id`, several CHECKs. V2: drops `chk_vital_type` only (enum). |
| household | V1–V4 | V1: `calendar_event` + `inventory_item`, enum CHECKs. V2: `goal`. V3: drops all remaining enum CHECKs (event_type, source_platform, unit, goal_status). V4: adds `is_consumed BOOLEAN` to `inventory_item`. |
| projections | V1 | `dashboard_snapshot` — composite PK `(profile_id, snapshot_key)`, FK to `profile.profile` ON DELETE CASCADE. |

### 0.2 Naming inconsistencies found (cross-domain audit)

| Inconsistency | Where | Verdict |
|---|---|---|
| `dob` (profile) vs no equivalent elsewhere | `profile.profile.dob` | Not actually inconsistent with anything else in-repo — flagged in the task prompt as a prior finding (`date_of_birth` vs `dob`), but no other domain has a birth-date column to compare against. Standardize on `dob` going forward (shorter, already used) — no change needed, just confirming the naming standard for future columns. |
| `account_name` / `institution_name` (wealth.account) vs `asset_name` (wealth.physical_asset) vs `item_name` (household.inventory_item) vs `goal_name` (household.goal) vs `title` (household.calendar_event) vs `full_name` (profile.profile) | Cross-domain "name of the thing" column | **Inconsistent.** Six different names for the same semantic role (a human-readable label for the primary entity). No technical reason for the variance — appears organic (each domain author picked their own word). |
| `email_address` (profile.admin, profile.profile) — consistent | profile domain only | No inconsistency — both profile tables agree. Good precedent to extend. |
| `created_at` present on every table except `wealth.account` in V1 (added implicitly, confirmed present), `profile.profile` (present) | Audited all 12 tables | Actually consistent — every table has `created_at TIMESTAMPTZ NOT NULL DEFAULT now()`. No drift found here, despite being a common drift point in other codebases. Good. |
| `is_active` (wealth.account, wealth.physical_asset, profile.admin) vs no equivalent soft-delete flag on `household.calendar_event`, `household.inventory_item`, `household.goal`, `health.vital_reading`, `health.doctor_visit` | Cross-domain soft-delete pattern | **Inconsistent, but not necessarily wrong** — household/health tables use hard delete (`DELETE /v1/...` endpoints exist and are used), while wealth/profile use soft-delete (`is_active` toggle). This is a product-behavior difference, not an accidental naming drift — out of scope to "fix" here since changing delete semantics is a behavior change, not a schema consolidation. Flagged, not changed. |
| `profile_id UUID` type consistent everywhere it exists | All 8 tables that carry it | No inconsistency. |

**VARCHAR length inconsistencies found:**

| Column role | Length used | Where |
|---|---|---|
| "name of the primary entity" | `VARCHAR(150)` | `profile.profile.full_name`, `profile.admin.display_name` |
| | `VARCHAR(200)` | `household.calendar_event.title`, `household.inventory_item.item_name`, `household.goal.goal_name` |
| | `VARCHAR(100)` | `wealth.account.account_name`, `wealth.account.institution_name`, `wealth.physical_asset.asset_name`, `wealth.physical_asset.make`, `wealth.physical_asset.model` |
| "email" | `VARCHAR(255)` | `profile.profile.email_address`, `profile.admin.email_address` — consistent |
| "generic discriminator/type" | `VARCHAR(30)` | `profile.profile.relation_to_admin`, `profile.profile.gender`, `health.vital_reading.vital_type` |
| | `VARCHAR(50)` | `wealth.account.account_type`, `wealth.physical_asset.asset_type`, `wealth.physical_asset.registration_type`, `household.calendar_event.event_type`, `household.inventory_item.source_platform`, `household.inventory_item.category`, `wealth.upload_error_log.error_type` |
| | `VARCHAR(20)` | `wealth.statement_upload.status`, `health.vital_reading.unit`, `household.inventory_item.unit`, `household.goal.status` |
| | `VARCHAR(10)` | `profile.profile.blood_type`, `wealth.transaction.txn_type`, `wealth.account.currency` |

No stated reason anywhere for choosing 30 vs 50 vs 100 vs 150 vs 200 for what is structurally the same kind of column (a name label or a discriminator string). This is real, confirmed inconsistency — addressed per-domain below with one rule: **since discriminators are enforced at the contract/enum layer, not the DB, their VARCHAR length is a non-binding safety margin, not a business rule** — standardize to remove arbitrary variance.

### 0.3 The `wealth.transaction.profile_id` question (Q33) — investigated, recommendation given

**Finding, confirmed against both SQL and Java:**
- `application/flyway/wealth/V1__init_ledger.sql` — `wealth.transaction` has **no `profile_id` column**. Only `account_id UUID NOT NULL FK → wealth.account(id)`.
- `application/domain/wealth/adapters/.../TransactionEntity.java` — JPA mapping confirms this: no `profile_id` `@Column` exists on the entity. `AccountEntity.java` does have `@Column(name = "profile_id", ...)`.
- The actual runtime filter (`TransactionPanacheRepository.findByAccountId`/`existsByDeduplicationKey`) resolves `profile_id` **transitively** — via a subquery/join against `AccountEntity.profileId` — exactly as ADR-019 documents for the domain-entity-field trade-off, and as v0.5 Phase 0 (Q12/Q28) hardened at the adapter-query level.
- **Correction to the task prompt's framing and to `documents/domain-state/wealth.md`:** the domain-state file's schema table lists `transaction` as having its own `profile_id UUID FK` column — this is **incorrect**, confirmed by both the migration and the entity. This is itself a documentation-drift finding, logged in the companion `documents/architecture-review-2026-07.md` Section 2.2, and corrected in this plan's Phase 2 design.

**Q33 recommendation (architect's answer, not left open):**

Do **NOT** denormalize `profile_id` onto `wealth.transaction` as a direct column. Recommendation: **"must include profileId" means every domain's root/primary aggregate table gets a direct column — child/detail tables that are unambiguously owned by exactly one already-scoped parent row do not need their own copy.**

Rationale:
1. `wealth.transaction` is a true child of `wealth.account` — every transaction has exactly one `account_id`, and every account has exactly one `profile_id`. There is no scenario where a transaction's effective owner differs from its account's owner (accounts aren't reassigned between profiles). Denormalizing would introduce a *derived, redundant* value that could theoretically drift from its source of truth (`account.profile_id`) if a future bug ever wrote a mismatched value — a real new failure mode that doesn't exist today.
2. This matches the exact precedent ADR-019 already accepted for the *domain entity* layer ("Transaction is scoped transitively via its parent Account") — extending the same reasoning to the *schema* layer is consistent, not a new policy.
3. It keeps the existing, already-tested, already-adapter-verified filter mechanism (`TransactionPanacheRepository`'s subquery join, hardened in v0.5 Phase 0 / Q12/Q28) working unchanged. Changing this now, under a "no FK" mandate (Phase 2's other change), would mean simultaneously restructuring the query layer for zero behavior benefit while removing the very FK that makes the transitive join provably correct.
4. Applying this consistently: **root tables that get a direct `profile_id` column** — `wealth.account`, `wealth.physical_asset`, `health.vital_reading`, `health.doctor_visit`, `household.calendar_event`, `household.inventory_item`, `household.goal`. **Tables that do NOT get one** — `wealth.transaction` (child of account), `wealth.statement_upload` (child of account), `wealth.upload_error_log` (child of statement_upload). `projections.dashboard_snapshot` already has its own `profile_id` as part of its PK — unaffected either way, it's not owned by another table.

This resolves Q33 with a stated rule instead of a per-table ad hoc choice.

### 0.4 Verification baseline (what "same effective schema shape" means for this plan)

Before any phase is executed, capture:
```bash
# Column-level baseline per domain, run against a live migrated dev DB:
psql -U app_user -d app_db -c "\d+ profile.profile" > baseline_profile.txt
psql -U app_user -d app_db -c "\d+ profile.admin" >> baseline_profile.txt
psql -U app_user -d app_db -c "\d+ wealth.account" > baseline_wealth.txt
psql -U app_user -d app_db -c "\d+ wealth.transaction" >> baseline_wealth.txt
psql -U app_user -d app_db -c "\d+ wealth.statement_upload" >> baseline_wealth.txt
psql -U app_user -d app_db -c "\d+ wealth.upload_error_log" >> baseline_wealth.txt
psql -U app_user -d app_db -c "\d+ wealth.physical_asset" >> baseline_wealth.txt
psql -U app_user -d app_db -c "\d+ health.vital_reading" > baseline_health.txt
psql -U app_user -d app_db -c "\d+ health.doctor_visit" >> baseline_health.txt
psql -U app_user -d app_db -c "\d+ household.calendar_event" > baseline_household.txt
psql -U app_user -d app_db -c "\d+ household.inventory_item" >> baseline_household.txt
psql -U app_user -d app_db -c "\d+ household.goal" >> baseline_household.txt
psql -U app_user -d app_db -c "\d+ projections.dashboard_snapshot" > baseline_projections.txt
```
Every phase's "Verification" step below diffs the post-consolidation `\d+` output against these baselines, checking **column name + type** match (constraints are expected to differ — that's the point of Phase 2's FK/CHECK removal). Then run `./gradlew test` — the existing JPA `@Column(name=...)` mappings and adapter Testcontainers tests are the real proof that Hibernate can still bind to the consolidated schema; a column rename that breaks a `@Column` mapping fails loudly at test time, not silently.

---

## Phase 1 — Profile Domain Consolidated Script

**File to produce (when executed):** `application/flyway/profile/V1__init_profile_consolidated.sql` (replaces V1+V2+V3; old files deleted per the in-place-edit override).

### Before (3 files, summarized)
- `admin(id, display_name, email_address, is_active, created_at)` + `policy_settings JSONB` (added V3)
- `profile(id, admin_id FK, full_name, dob, relation_to_admin, email_address, gender, blood_type, metadata JSONB, is_active, created_at)`
- FK `profile.admin_id → admin.id`
- Unique index `uq_admin_self_profile` (structural, kept per current philosophy)
- Unique index `uq_admin_email` (structural)
- No CHECK constraints remain today (already fully migrated off enum CHECKs by V2)

### After (one script)

```sql
CREATE TABLE profile.admin (
    id              UUID         NOT NULL DEFAULT gen_random_uuid(),
    display_name    VARCHAR(150) NOT NULL,
    email_address   VARCHAR(255),
    policy_settings JSONB        NOT NULL DEFAULT '{}'::jsonb,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_admin PRIMARY KEY (id)
    -- NO uq_admin_email UNIQUE constraint per Phase-2-wide "no DB constraints" instruction (see Section 2)
    -- NO FK from profile.profile below, per the same instruction
);

CREATE TABLE profile.profile (
    id                UUID         NOT NULL DEFAULT gen_random_uuid(),
    admin_id          UUID,                     -- NOT a DB FK anymore (see Section 2) — app-layer enforced
    full_name         VARCHAR(150) NOT NULL,
    dob               DATE         NOT NULL,
    relation_to_admin VARCHAR(30)  NOT NULL,    -- unchanged length; contract enum enforces values
    email_address     VARCHAR(255),
    gender            VARCHAR(30),
    blood_type        VARCHAR(10),
    metadata          JSONB        NOT NULL DEFAULT '{}'::jsonb,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT pk_profile PRIMARY KEY (id)
    -- NO fk_profile_admin, NO uq_admin_self_profile unique index — see Section 2 mitigation instead
);
```

### Naming/type changes and rationale
- **No column renames needed** — `dob`, `full_name`, `email_address`, `blood_type`, `relation_to_admin` are already the shortest/clearest names in the repo and match the "email_address consistent" precedent noted in Phase 0.
- **No VARCHAR length changes** — profile's lengths (150/255/30/10) are internally consistent already; leaving as the reference standard other domains should have matched (see Phase 2/4 "name" column consolidation below, using `VARCHAR(150)` as the household-wide standard for "name of a thing" columns going forward. Not retrofitting profile itself since it's already the good example).
- **Constraint removal (Phase-wide instruction):** `fk_profile_admin`, `uq_admin_email`, `uq_admin_self_profile` all dropped. This is the single biggest structural change to this domain — see Section 2 for the specific risk and mitigation recommended for `uq_admin_self_profile` in particular (it currently prevents a second SELF profile per admin; losing it is a real behavior change, not just a safety-net removal, since nothing else in the application enforces it today).

### Verification
1. `psql -U app_user -d app_db -c "\d+ profile.profile"` and `\d+ profile.admin"` — confirm column names/types match `baseline_profile.txt` exactly (constraints will differ, expected).
2. `./gradlew :application:domain:profile:adapters:test` — `AdminResourceTest`, `ProfileResourceTest`, `AdminServiceTest`, `ProfileServiceTest`, and the Testcontainers-backed repository tests must all still pass. These tests reference columns by JPA `@Column` mapping, not raw SQL, so a rename would fail here first.
3. Manually attempt a duplicate-SELF-profile creation via `POST /v1/profiles` twice for the same `admin_id` with `relation_to_admin=SELF` — confirm the **application-layer** check (`ProfileRepository.existsSelfProfile`, added this session per domain-state notes) still rejects it with 409, now that the DB-level `uq_admin_self_profile` index is gone. This is the concrete mitigation-in-action check for the one risk called out above.

---

## Phase 2 — Wealth Domain Consolidated Script

**File to produce:** `application/flyway/wealth/V1__init_wealth_consolidated.sql` (replaces V1–V7).

### Before (7 files, summarized — see Phase 0.1 table)

### After — table-by-table

**`wealth.account`** (root aggregate — gets direct `profile_id`, per Q33 rule)

| Before (post-V6, effective) | After | Change + rationale |
|---|---|---|
| `id UUID PK` | `id UUID PK` | unchanged |
| `profile_id UUID` (FK, no CHECK) | `profile_id UUID` (no FK) | FK dropped — Section 2 |
| `institution_name VARCHAR(100)` | `institution_name VARCHAR(150)` | Standardized to the household-wide "name" length (150, matching profile's precedent) — was arbitrarily 100 |
| `account_name VARCHAR(100)` | `account_name VARCHAR(150)` | Same standardization |
| `account_type VARCHAR(50)` | `account_type VARCHAR(50)` | unchanged — discriminator, 50 is now the cross-domain discriminator standard (see below) |
| `currency VARCHAR(10)` | `currency VARCHAR(10)` | unchanged |
| `is_active BOOLEAN` | `is_active BOOLEAN` | unchanged |
| `created_at TIMESTAMPTZ` | `created_at TIMESTAMPTZ` | unchanged |
| `opening_balance NUMERIC(19,4)` | `opening_balance NUMERIC(19,4)` | unchanged |
| `credit_limit NUMERIC(19,4)` | `credit_limit NUMERIC(19,4)` | unchanged |
| `interest_rate NUMERIC(7,4)` | `interest_rate NUMERIC(7,4)` | unchanged |
| `emi_amount NUMERIC(19,4)` | `emi_amount NUMERIC(19,4)` | unchanged |
| `metadata JSONB` | `metadata JSONB` | unchanged |
| — | — | `chk_account_type` (already dropped in V4) stays dropped; no other CHECK existed here to remove |

**`wealth.transaction`** (child of account — no direct `profile_id`, per Q33 rule)

| Before | After | Change + rationale |
|---|---|---|
| `id UUID PK` | `id UUID PK` | unchanged |
| `account_id UUID NOT NULL` (FK) | `account_id UUID NOT NULL` (no FK) | FK dropped — Section 2. **NOT NULL kept** — this is a structural invariant (a transaction with no account is meaningless), and the instruction targets FK/CHECK, not NOT NULL |
| `upload_id UUID` (nullable, FK — V7) | `upload_id UUID` (nullable, no FK) | FK dropped; nullability unchanged (V7's manual-transaction rationale still applies) |
| `txn_date DATE NOT NULL` | `txn_date DATE NOT NULL` | unchanged |
| `amount NUMERIC(19,4) NOT NULL, CHECK(amount>=0)` | `amount NUMERIC(19,4) NOT NULL` — **CHECK dropped** | Per instruction, all CHECKs removed including business-rule ones like `amount >= 0`. **This is the one most worth flagging** — see Section 2, this is exactly the kind of business-rule CHECK `CLAUDE.md` currently says to keep. Mitigation: `Transaction` domain entity's factory/validation already rejects negative amounts at construction time (confirmed pattern used elsewhere) — recommend confirming this exists before executing, or adding it in the same PR that drops the CHECK. Flagged as **Q45** below since a systematic sweep of "does every domain entity already validate what its dropped CHECK used to guarantee" wasn't verifiable from schema files alone. |
| `txn_type VARCHAR(10) NOT NULL` (CHECK dropped already in V5) | `txn_type VARCHAR(10) NOT NULL` | unchanged — already enum-only |
| `description TEXT NOT NULL` | `description TEXT NOT NULL` | unchanged |
| `metadata JSONB` | `metadata JSONB` | unchanged |
| `created_at TIMESTAMPTZ` | `created_at TIMESTAMPTZ` | unchanged |
| `uq_transaction_dedup UNIQUE(account_id, txn_date, amount, txn_type, description)` | **dropped** | Per instruction — no UNIQUE either (instruction says "no FK, no CHECK" — UNIQUE is a distinct constraint type not explicitly named, but the spirit of "validation moves to contract/app layer" plus the product owner's stated direction of minimal DB constraints leans toward dropping it too; flagged explicitly as **Q46** below rather than silently keeping or dropping since UNIQUE wasn't explicitly named in the instruction and this is a real dedup safety net, not merely an enum check) |

**`wealth.statement_upload`** (child of account — no direct `profile_id`)

| Before | After | Change + rationale |
|---|---|---|
| `id UUID PK` | `id UUID PK` | unchanged |
| `account_id UUID NOT NULL` (FK) | `account_id UUID NOT NULL` (no FK) | FK dropped |
| `file_name VARCHAR(255)` | `file_name VARCHAR(255)` | unchanged — this is a filename, not an entity-name; 255 is appropriate and distinct from the "entity name" standardization above |
| `upload_date TIMESTAMPTZ` | `upload_date TIMESTAMPTZ` | unchanged |
| `status VARCHAR(20)` (CHECK dropped already V5) | `status VARCHAR(20)` | unchanged |

**`wealth.upload_error_log`** (child of statement_upload — no direct `profile_id`)

| Before | After | Change + rationale |
|---|---|---|
| `id UUID PK` | `id UUID PK` | unchanged |
| `upload_id UUID NOT NULL` (FK, CASCADE) | `upload_id UUID NOT NULL` (no FK) | FK dropped. **Note:** losing `ON DELETE CASCADE` specifically removes the "rollback an upload → error log rows vanish automatically" mechanism described in V3's own header comment as "no orphan cleanup required." Application-layer cleanup must now explicitly delete error-log rows when an upload is rolled back — flagged in Section 2 mitigation list, not just a generic FK loss. |
| `error_type VARCHAR(50)` (CHECK dropped already V5) | `error_type VARCHAR(50)` | unchanged |
| `missing_columns TEXT[]` | `missing_columns TEXT[]` | unchanged |
| `error_detail TEXT NOT NULL` | `error_detail TEXT NOT NULL` | unchanged |
| `created_at TIMESTAMPTZ` | `created_at TIMESTAMPTZ` | unchanged |

**`wealth.physical_asset`** (root aggregate — gets direct `profile_id`)

| Before | After | Change + rationale |
|---|---|---|
| `id UUID PK` | `id UUID PK` | unchanged |
| `profile_id UUID` (FK) | `profile_id UUID` (no FK) | FK dropped |
| `asset_name VARCHAR(100)` | `asset_name VARCHAR(150)` | Standardized to 150 (same "entity name" rationale as `account_name`) |
| `asset_type VARCHAR(50)` (CHECK dropped V5) | `asset_type VARCHAR(50)` | unchanged |
| `make VARCHAR(100)` | `make VARCHAR(100)` | unchanged — not a discriminator or the primary entity name, leave as-is |
| `model VARCHAR(100)` | `model VARCHAR(100)` | unchanged, same reasoning |
| `registration_number VARCHAR(50)` (UNIQUE) | `registration_number VARCHAR(50)` — **UNIQUE dropped** | Per instruction. This is the natural business key (a vehicle plate cannot legally repeat) — flagged as a real risk in Section 2, application layer already has a pre-check (`PhysicalAssetService` confirmed to validate uniqueness before insert per domain-state notes) so the behavior is preserved at the app layer even though the DB safety net is gone. |
| `registration_type VARCHAR(50)` (CHECK dropped V5) | `registration_type VARCHAR(50)` | unchanged |
| `metadata JSONB` | `metadata JSONB` | unchanged |
| `is_active BOOLEAN` | `is_active BOOLEAN` | unchanged |
| `created_at TIMESTAMPTZ` | `created_at TIMESTAMPTZ` | unchanged |

### Naming/type changes summary for wealth
- `account_name`, `institution_name`, `asset_name`: `VARCHAR(100) → VARCHAR(150)` (standardize "entity name" columns to 150 across the project, matching `profile.full_name`/`admin.display_name`).
- No other renames — wealth's column names (`account_type`, `txn_type`, `txn_date`, `opening_balance`, etc.) are already clear and don't collide with the cross-domain "name" ambiguity found in Phase 0.

### Verification
1. Diff `\d+` output for all 5 tables against `baseline_wealth.txt` — column names/types must match; constraints will differ per the changes above.
2. `./gradlew :application:domain:wealth:adapters:test` — this is the strongest check available: `AccountResourceTest`, `TransactionResourceTest`, `PhysicalAssetResourceTest`, plus the Testcontainers repository tests (`findByAccountId_profileFilter_blocksCrossProfileAccess`, `existsByDeduplicationKey_profileFilter_blocksCrossProfileMatch`) must still pass — these tests exercise the transitive `profile_id` join through `account_id`, which is exactly the mechanism Q33's recommendation preserves unchanged.
3. Manually insert a negative-amount transaction via `POST /v1/accounts/{id}/transactions` and confirm the **application layer** rejects it (per the Q45 mitigation) now that `chk_txn_amount` no longer exists in the DB. If it does NOT reject it, this is a launch-blocking gap, not a nice-to-have — do not proceed to cutover (Phase 6) until confirmed.
4. Manually insert a duplicate `registration_number` via `POST /v1/physical-assets` twice and confirm `PhysicalAssetService`'s existing app-layer uniqueness check still returns 409 now that `uq_registration_number` is gone from the DB.

---

## Phase 3 — Health Domain Consolidated Script

**File to produce:** `application/flyway/health/V1__init_health_consolidated.sql` (replaces V1+V2).

### Table-by-table

**`health.vital_reading`** (root aggregate — direct `profile_id`, already present and NOT NULL)

| Before | After | Change + rationale |
|---|---|---|
| `id UUID PK` | `id UUID PK` | unchanged |
| `profile_id UUID NOT NULL` (FK) | `profile_id UUID NOT NULL` (no FK) | FK dropped; **NOT NULL kept** — health data with no owner is meaningless per the table's own original comment, and NOT NULL isn't in scope of the "no FK/CHECK" instruction |
| `vital_type VARCHAR(30)` (CHECK dropped V2) | `vital_type VARCHAR(50)` | **Length changed 30→50** — standardizes to the cross-domain discriminator length (50), matching `account_type`/`asset_type`/`event_type`/`source_platform`. 30 was arbitrarily tighter than every other discriminator column in the project with no stated reason. |
| `reading_date DATE` | `reading_date DATE` | unchanged |
| `value_primary NUMERIC(8,2) NOT NULL, CHECK(>0)` | `value_primary NUMERIC(8,2) NOT NULL` — **CHECK dropped** | Per instruction — this is a business-rule CHECK (biometric values are always positive), same category of risk as wealth's `amount >= 0`. Flagged in Section 2; needs the same "does the domain entity already validate this" confirmation — logged as part of **Q45** (generalized, not health-specific). |
| `value_secondary NUMERIC(8,2)` (nullable) | `value_secondary NUMERIC(8,2)` | unchanged |
| `unit VARCHAR(20)` | `unit VARCHAR(20)` | unchanged — genuinely short values (`kg`, `mmHg`, `bpm`), 20 is reasonable and distinct from the "type discriminator" standardization |
| `notes TEXT` | `notes TEXT` | unchanged |
| `metadata JSONB` | `metadata JSONB` | unchanged |
| `created_at TIMESTAMPTZ` | `created_at TIMESTAMPTZ` | unchanged |
| `chk_bp_secondary_value CHECK(...)` | **dropped** | Per instruction — this is the "if BLOOD_PRESSURE then value_secondary required" structural rule. This is the strongest case in the whole audit for a mitigation being necessary: it's a conditional-field business rule, not a simple range check, so the domain-layer replacement must specifically replicate "vital_type == BLOOD_PRESSURE implies value_secondary present" — flagged in **Q45**. |

**`health.doctor_visit`** (root aggregate — direct `profile_id`, already present and NOT NULL)

| Before | After | Change + rationale |
|---|---|---|
| `id UUID PK` | `id UUID PK` | unchanged |
| `profile_id UUID NOT NULL` (FK) | `profile_id UUID NOT NULL` (no FK) | FK dropped |
| `from_date DATE NOT NULL` | `from_date DATE NOT NULL` | unchanged |
| `to_date DATE` (nullable), `chk_visit_dates CHECK(to_date>=from_date)` | `to_date DATE` — **CHECK dropped** | Per instruction — same category as `end_date >= start_date` elsewhere. Flagged in Q45. |
| `visited_doctor BOOLEAN NOT NULL DEFAULT TRUE` | `visited_doctor BOOLEAN NOT NULL DEFAULT TRUE` | unchanged |
| `doctor_name VARCHAR(100)`, `chk_doctor_name_required CHECK(...)` | `doctor_name VARCHAR(150)` — **CHECK dropped**, length standardized | Length: 100→150 (entity-name standardization). CHECK drop: same conditional-field-rule risk as `chk_bp_secondary_value` above — flagged in Q45. |
| `hospital_name VARCHAR(200)` | `hospital_name VARCHAR(200)` | unchanged — this is a facility name, arguably should also standardize to 150, but 200 is already the household-domain "title/name" precedent (`calendar_event.title`, `inventory_item.item_name`, `goal.goal_name` are all 200) — see Phase 4 for why 200 wins as the actual cross-domain standard once household's 3 uses are counted alongside wealth/health's 2. **Reconciliation note:** this creates a 3-way split (150 vs 200) that this plan does not fully resolve — flagged as **Q47** below rather than picking arbitrarily between the profile-domain precedent (150) and the household-domain precedent (200). |
| `speciality VARCHAR(100)` | `speciality VARCHAR(100)` | unchanged — not an entity name, a discriminator-adjacent field; left as-is |
| `symptoms TEXT`, `diagnosis TEXT`, `notes TEXT` | unchanged | unchanged |
| `follow_up_date DATE` | `follow_up_date DATE` | unchanged |
| `metadata JSONB` | `metadata JSONB` | unchanged |
| `created_at TIMESTAMPTZ` | `created_at TIMESTAMPTZ` | unchanged |

### Verification
1. Diff `\d+` output against `baseline_health.txt`.
2. `./gradlew :application:domain:health:adapters:test` — `VitalReadingResourceTest`, `DoctorVisitResourceTest`, `VitalReadingPanacheRepositoryTest`, `DoctorVisitPanacheRepositoryTest` must pass.
3. Manually test: create a BLOOD_PRESSURE vital reading with `value_secondary = null` via the API — confirm the domain/service layer still rejects it (per Q45 mitigation) now that `chk_bp_secondary_value` is gone from the DB. If application code does not already enforce this, this is launch-blocking for Phase 3 same as Phase 2's amount check.
4. Manually test: create a doctor visit with `visited_doctor = true` and `doctor_name = null` — confirm app-layer rejection still works.

---

## Phase 4 — Household Domain Consolidated Script

**File to produce:** `application/flyway/household/V1__init_household_consolidated.sql` (replaces V1–V4).

### Table-by-table

**`household.calendar_event`** (root aggregate — direct `profile_id`)

| Before | After | Change + rationale |
|---|---|---|
| `id UUID PK` | `id UUID PK` | unchanged |
| `profile_id UUID` (nullable, FK) | `profile_id UUID` (nullable, no FK) | FK dropped. Nullability unchanged — household's "NULL = admin profile" convention predates this plan and is a product-behavior choice, not addressed here. |
| `title VARCHAR(200)` | `title VARCHAR(200)` | unchanged — this becomes the winning cross-domain "entity name" standard once reconciled with Q47 below |
| `event_type VARCHAR(50)` (CHECK dropped V3) | `event_type VARCHAR(50)` | unchanged |
| `start_date DATE NOT NULL` | `start_date DATE NOT NULL` | unchanged |
| `end_date DATE`, `chk_event_dates CHECK(end_date>=start_date)` | `end_date DATE` — **CHECK dropped** | Per instruction — flagged in Q45 |
| `location VARCHAR(200)` | `location VARCHAR(200)` | unchanged |
| `notes TEXT` | `notes TEXT` | unchanged |
| `metadata JSONB` | `metadata JSONB` | unchanged |
| `created_at TIMESTAMPTZ` | `created_at TIMESTAMPTZ` | unchanged |

**`household.inventory_item`** (root aggregate — direct `profile_id`)

| Before | After | Change + rationale |
|---|---|---|
| `id UUID PK` | `id UUID PK` | unchanged |
| `profile_id UUID` (nullable, FK) | `profile_id UUID` (nullable, no FK) | FK dropped |
| `item_name VARCHAR(200)` | `item_name VARCHAR(200)` | unchanged |
| `quantity NUMERIC(10,3) NOT NULL`, `chk_quantity_positive CHECK(>0)` | `quantity NUMERIC(10,3) NOT NULL` — **CHECK dropped** | Per instruction — flagged in Q45 |
| `unit VARCHAR(20)` (CHECK dropped V3) | `unit VARCHAR(20)` | unchanged |
| `source_platform VARCHAR(50)` (CHECK dropped V3) | `source_platform VARCHAR(50)` | unchanged |
| `purchase_date DATE NOT NULL` | `purchase_date DATE NOT NULL` | unchanged |
| `category VARCHAR(50)` | `category VARCHAR(50)` | unchanged |
| `metadata JSONB` | `metadata JSONB` | unchanged |
| `is_consumed BOOLEAN NOT NULL DEFAULT false` (V4) | `is_consumed BOOLEAN NOT NULL DEFAULT false` | unchanged — folded into the base table definition instead of a separate `ALTER` |
| `created_at TIMESTAMPTZ` | `created_at TIMESTAMPTZ` | unchanged |

**`household.goal`** (root aggregate — direct `profile_id`)

| Before | After | Change + rationale |
|---|---|---|
| `id UUID PK` | `id UUID PK` | unchanged |
| `profile_id UUID` (nullable, FK) | `profile_id UUID` (nullable, no FK) | FK dropped |
| `goal_name VARCHAR(200)` | `goal_name VARCHAR(200)` | unchanged |
| `target_amount NUMERIC(19,2) NOT NULL`, `chk_goal_target_positive CHECK(>0)` | `target_amount NUMERIC(19,2) NOT NULL` — **CHECK dropped** | Per instruction — flagged in Q45 |
| `current_amount NUMERIC(19,2) NOT NULL DEFAULT 0.00`, `chk_goal_current_non_negative CHECK(>=0)` | `current_amount NUMERIC(19,2) NOT NULL DEFAULT 0.00` — **CHECK dropped** | Per instruction — flagged in Q45 |
| `monthly_saving NUMERIC(19,2)` | `monthly_saving NUMERIC(19,2)` | unchanged |
| `target_date DATE` | `target_date DATE` | unchanged |
| `status VARCHAR(20)` (CHECK dropped V3) | `status VARCHAR(20)` | unchanged |
| `notes TEXT` | `notes TEXT` | unchanged |
| `created_at TIMESTAMPTZ` | `created_at TIMESTAMPTZ` | unchanged |

### Q47 resolution — the 150-vs-200 "entity name" length split

Household consistently uses 200 for its 3 name-like columns (`title`, `item_name`, `goal_name`); profile uses 150 for its 2 (`full_name`, `display_name`); wealth's account/asset names are being standardized to 150 in Phase 2. This plan recommends **200 as the single project-wide standard** for "human-readable label of the primary entity," not 150, reversing the Phase 2 wealth decision above — because:
1. Household already has 3 independent uses of 200 (a majority among the 5 "entity name" columns across the audited domains once household is counted), vs. profile's 2 uses of 150.
2. 200 is a strict superset — any value that fits in 150 also fits in 200, so widening wealth's columns from 100→200 (instead of 100→150 as drafted in Phase 2) loses nothing and matches more of the existing codebase.
3. Financial account/institution names in the real world (e.g., long joint-holder legal names, or verbose institution names) are more likely to need >150 chars than a person's `full_name` is.

**Action:** Revise Phase 1 and Phase 2 above to use `VARCHAR(200)` instead of `VARCHAR(150)` for `account_name`, `institution_name`, `asset_name` before execution. Phase 1's `profile.full_name`/`admin.display_name` are left at 150 unchanged (not retrofitted) since widening them is a strictly separate, lower-priority decision with no forcing function — flag as optional cleanup, not required by this consolidation.

### Verification
1. Diff `\d+` output against `baseline_household.txt`.
2. `./gradlew :application:domain:household:adapters:test` — `CalendarEventServiceTest`, `InventoryItemServiceTest`, `GoalServiceTest`, domain unit tests (`CalendarEventTest`, `InventoryItemTest`, `GoalTest`) must pass — the domain unit tests specifically are the best proxy for "does the domain layer already enforce what the dropped CHECKs used to guarantee," since they test the entity's own validation logic independent of the DB.
3. Manually test: create a goal with `target_amount = -100` via `POST /v1/goals` — confirm app-layer rejection (Q45 mitigation) now that `chk_goal_target_positive` is gone.
4. Manually test: create a calendar event with `end_date < start_date` — confirm app-layer rejection now that `chk_event_dates` is gone.

---

## Phase 5 — Projections Schema Consolidated Script

**File to produce:** `application/flyway/projections/V1__init_projections_consolidated.sql` (only 1 file exists today — this "consolidation" is really just the FK-removal pass, no naming/type changes needed).

| Before | After | Change + rationale |
|---|---|---|
| `profile_id UUID NOT NULL` (FK, ON DELETE CASCADE) | `profile_id UUID NOT NULL` (no FK) | FK dropped. **Note:** losing `ON DELETE CASCADE` here means deleting a profile no longer auto-removes its dashboard snapshots — they become orphaned rows keyed to a profile_id that no longer exists. Since `profile.profile` uses soft-delete (`is_active=false`) in practice, not hard delete, this is a low-probability scenario today — but flagged in Section 2 as a specific instance of the general FK-removal risk. |
| `snapshot_key VARCHAR(100) NOT NULL` | `snapshot_key VARCHAR(100) NOT NULL` | unchanged — this is a controlled, code-defined constant (`SnapshotKey.java`), not a name-of-entity or a free-form discriminator; 100 is fine |
| `payload JSONB NOT NULL` | `payload JSONB NOT NULL` | unchanged |
| `calculated_at TIMESTAMPTZ NOT NULL DEFAULT now()` | `calculated_at TIMESTAMPTZ NOT NULL DEFAULT now()` | unchanged |
| `pk_dashboard_snapshot PRIMARY KEY (profile_id, snapshot_key)` | **unchanged — PK kept** | PKs are not FK or CHECK constraints; the instruction does not mention removing primary keys, and a table with no PK at all would break the existing `ON CONFLICT (profile_id, snapshot_key) DO UPDATE` UPSERT pattern that `ProjectionCalculationEngine`/ADR-013 depend on structurally, not just as a safety net. Confirmed in scope to keep. |

### Verification
1. Diff `\d+` output against `baseline_projections.txt`.
2. `./gradlew :application:web-gateway:test` — `ProjectionResourceTest` and all `ProjectionCalculationEngine` step tests must pass; the UPSERT pattern must still function since the PK is unchanged.
3. Manually run a dashboard refresh (`POST /v1/projections/refresh/{profileId}`) twice in a row and confirm the second call still updates (not duplicates) the snapshot row — proves the PK-based `ON CONFLICT` target still works without the dropped FK.

---

## Phase 6 — Cutover

**Only relevant once a human explicitly authorizes moving from plan to execution (see Q31/Q32 in `OpenQuestions.md` — both still awaiting explicit product-owner sign-off as of this plan's writing).**

1. **Order:** profile → wealth → health → household → projections (same dependency order as today's startup order — profile has no FK dependency on other domains in Phase 1's script since all FKs are dropped project-wide, but keeping this order avoids any confusion and matches existing tooling/scripts that assume it).
2. **Per-developer local DB reset required** (Q32's flagged consequence): every developer running a local Postgres instance must `DROP SCHEMA profile, wealth, health, household, projections CASCADE;` (or drop and recreate `app_db` entirely) and re-run `00_bootstrap.sql` + the new consolidated `V1__` files fresh. `flyway repair` does **not** help here since the migration checksums for the old `V1__…Vn__` chain no longer exist on disk to repair against — this must be communicated explicitly to every developer before merging the consolidated migrations, not discovered by someone's local Flyway erroring out.
3. **Test-seed files** (`application/flyway/test-seed/{domain}/R__seed_*_test_data.sql`) reference specific columns from the current schema — re-verify each seed file's `INSERT` statements still match the consolidated column list (names unchanged except the `VARCHAR` length bumps, which don't affect INSERT syntax) before merging.
4. **Full-suite verification:** `./gradlew test` (all modules) + `ss` (local SonarQube pass) green, then manually smoke-test the full user flow (create profile → create account → upload statement → log a vital → create a calendar event → refresh dashboard) against a freshly-reset local DB, since this is the first time all 5 domains' schemas change simultaneously in one sitting.
5. **Documentation updates required at cutover, not before** (executing this plan is what triggers these, not the planning pass):
   - `CLAUDE.md` — update "DB constraint philosophy" section to reflect the new no-FK/no-CHECK policy (or revert this plan if Q31 comes back "no, keep FKs" — see Section 2).
   - `CLAUDE.md` — remove "never edit a committed migration" for historical migrations that were just replaced (or scope the note to "post-consolidation, this rule resumes" if the override is a one-time event, which is this plan's assumption).
   - Every `documents/domain-state/<domain>.md` — regenerate "Database Schema" tables from the new consolidated files (this also finally fixes the wealth `transaction.profile_id` drift documented in the companion architecture review).
   - `documents/ARCHITECTURE_DECISIONS.md` — new ADR recording this consolidation event, the FK/CHECK removal decision, and the Q33 root-vs-child profileId rule, so a future reader understands why the schema looks the way it does without archaeology through OpenQuestions.md.

---

## Section 2 — FK/CHECK Removal: Accepted Risk and Recommended Mitigation (Q31 context)

The instruction removes **all** FK and CHECK constraints, not just enum discriminators. This plan proceeds as instructed (per Q31's explicit direction), but the concrete risks being accepted are:

| Risk | Specific instance found in this audit | Recommended app-layer mitigation (still "not in DB," per instruction) |
|---|---|---|
| Orphaned `profile_id` rows | Every domain table's `profile_id` FK → `profile.profile(id)` — 7 tables affected | Before any insert that carries a `profile_id`, the owning adapter calls a `profileExists(profileId)` check against the profile service (gateway-mediated REST call for cross-service cases, or a direct repository existence check within the profile service itself for same-service cases). This is the exact shape of check the task's framing already anticipated. **Concrete implementation note:** for the 4 non-profile domain services, this means a new outbound REST call (`ProfileServiceClient`-equivalent) on every write path that sets `profile_id` — a real latency/availability cost that today's FK gets for free at zero runtime cost. Recommend this be scoped as a follow-up implementation task, not assumed free. |
| Orphaned child rows (`account_id`, `upload_id` FKs) | `wealth.transaction.account_id`, `wealth.statement_upload.account_id`, `wealth.transaction.upload_id`, `wealth.upload_error_log.upload_id` | Same-service existence checks are cheap (no network call, same DB) — recommend these are checked in the `*Service` layer before insert, same pattern as the existing `AccountRepository.findById` check already present in `StatementUploadService`. Lower risk than cross-service `profile_id` checks since no REST round-trip is needed. |
| Lost `ON DELETE CASCADE` behavior | `wealth.upload_error_log` (was CASCADE from `statement_upload`), `projections.dashboard_snapshot` (was CASCADE from `profile.profile`) | Application-layer cleanup: rollback/delete operations must explicitly delete dependent rows in the correct order before deleting the parent. This is a **behavior change** (today's cascade is automatic and atomic within one DB transaction; the app-layer equivalent is multiple statements, not automatically transactional unless the service wraps them in one `@Transactional` method — recommend confirming this explicitly during Phase 6 cutover testing, not assuming it's equivalent). |
| Lost business-rule CHECKs | `amount >= 0`, `value_primary > 0`, `end_date >= start_date`, `quantity > 0`, `target_amount > 0`, `current_amount >= 0`, `chk_bp_secondary_value`, `chk_doctor_name_required` — 8 distinct rules across 3 domains | These are exactly the class of constraint `CLAUDE.md` currently says to KEEP in the DB (they are structural invariants, not enum discriminators). Recommend, at minimum, an explicit audit (Q45 below) confirming every one of these 8 rules already has an equivalent domain-layer/service-layer check **before** executing Phase 2-4, not after — a silently-missing validation here is a correctness regression, not just a "less safe" posture. |
| Lost natural-key uniqueness | `uq_transaction_dedup`, `uq_registration_number`, `uq_admin_email`, `uq_admin_self_profile` | Same shape of risk as the CHECK removal — app-layer pre-checks needed. `uq_registration_number` and `uq_admin_self_profile` already have confirmed app-layer equivalents per domain-state notes (`PhysicalAssetService`, `ProfileRepository.existsSelfProfile`). `uq_transaction_dedup` and `uq_admin_email` need explicit confirmation — flagged in Q46. |

**Overall recommendation to the product owner (restating Q31 directly):** dropping FK constraints specifically is a materially different risk profile than dropping enum CHECKs. Enum CHECKs were redundant with contract-layer validation already (zero net risk). FK/business-CHECK removal introduces genuinely new failure modes (orphaned rows, silently-invalid data) that today's DB catches "for free," which the recommended app-layer mitigations above only approximate — with real gaps (partial-failure atomicity on multi-step deletes, and network-call cost/availability coupling on cross-service `profile_id` checks). This plan proceeds as instructed, but recommends re-confirming Q31 specifically calls out FK removal (not just CHECK removal) as intended before Phase 6 executes anything.

---

## New Open Questions Raised (Q44–Q48)

Appended to `documents/OpenQuestions.md` in the same format as existing entries — see that file for the live copy. Summarized here for this plan's own completeness:

- **Q44** — Should `wealth.account`/`institution_name`/`physical_asset.asset_name` widen to `VARCHAR(200)` (household's precedent) or `VARCHAR(150)` (profile's precedent)? This plan recommends 200 (Q47 reasoning above) but flags it as a real open choice, not a forced one.
- **Q45** — Before dropping the 8 business-rule CHECK constraints identified in Section 2, has each domain's application/domain layer been confirmed to already independently enforce the same rule? This audit could not verify Java-layer validation exhaustively from schema files alone — needs a dedicated pass (candidate: `quality-manager` or each domain developer agent) before Phase 2-4 execute, not just before Phase 6.
- **Q46** — Should `uq_transaction_dedup` and `uq_admin_email` (natural-key UNIQUE constraints, not enum CHECKs) be dropped along with FK/CHECK, or kept as a narrower exception? The instruction named "FK, CHECK" specifically, not UNIQUE — this plan defaulted to dropping them for consistency with the stated spirit, but it's a literal reading gap worth confirming explicitly.
- **Q47** — (Resolved within this plan, recorded here for traceability) 150 vs 200 for "entity name" columns — resolved as 200, household's majority precedent wins. See Phase 4.
- **Q48** — Should the per-developer local DB reset requirement (Phase 6, item 2) be automated via a `scripts/` helper (e.g. `reset-local-db.ps1`/`.sh`) rather than relying on every developer running the right manual `DROP SCHEMA` commands correctly? Recommend routing this to the `devops` agent once Phase 6 is authorized — out of scope for this architecture-planning pass but a real rollout risk if skipped.

---

## Related Documents

- `documents/architecture-review-2026-07.md` — Section 2.2/3.4 findings that fed this plan's Phase 0 audit
- `documents/OpenQuestions.md` — Q31, Q32, Q33 (context for this whole plan), Q44-Q48 (new, this plan)
- `documents/ARCHITECTURE_DECISIONS.md` — ADR-006, ADR-010, ADR-019 (all directly implicated by this plan's changes)
- `CLAUDE.md` — DB constraint philosophy and Flyway rules, both suspended for this effort per Q31/Q32, both needing an update at Phase 6 cutover
