# Epic 8 Implementation Plan — Automated Wealth Intelligence Engine

| | |
|---|---|
| **Type** | Implementation Plan |
| **Audience** | Developers, product owner |
| **Status** | Active — Q21-Q24 resolved; Q25 resolved via ADR-017 (household rollup) |
| **Last updated** | 2026-06-30 (household-rollup revision) |

## Objective

Turn `REQUIREMENTS_wealth_domain.md` Epic 8 (Use Cases 8.1–8.6) into a phased, buildable plan. Resolve the two architecture decisions the product owner delegated (expense granularity, joint account ownership). Sequence the 4 confirmed QA bugs into the phase that touches their code, instead of a separate bug-fix-first phase.

## Use Cases

- Before starting any Epic 8 development — confirm which phase you are in and what bugs must be fixed alongside it
- When scoping a sprint against `ROADMAP.md` — this plan is the detailed breakdown behind the "Financial Intelligence Engine" milestone line
- When the product owner asks "why does Phase 2 touch the net worth bug" — this doc has the mapping

---

## Scope Change — 2026-06-30: Household Rollup Is the Primary View

**Every Epic 8 output (net worth, category subtotals, goals, EMI tracking, validation) is a household-level rollup by default, not a per-individual-profile figure.** The product owner manages all family finances as head of household; his reference file is "Family Financial Data — Combined," not per-person. Full reasoning: `documents/ARCHITECTURE_DECISIONS.md` ADR-017. Resolution log: `documents/OpenQuestions.md` Q21 (financial-modeling policy) and Q25 (architecture mechanism).

**What this changes in this plan, precisely:**
- Phase 1's net-worth-formula fix (`computeNetWorth()` / the new `GET /v1/accounts/{accountId}/balance` endpoint) is **unchanged and still required** — it computes a correct *per-account* balance, which is the building block the rollup sums. This work is additive groundwork, not redone. See the new "Phase 1 — Family Rollup Aggregation Step" sub-section below for the one new piece of work this scope change adds to Phase 1.
- Phases 2-4 are otherwise unchanged in their per-phase deliverables (statement parsing, loan amortization, goals engine, validation engine) — only the *aggregation scope* of their outputs changes, from one profile_id to all profiles under one `admin_id`. The same mechanism (resolve household members via `ProfileServiceClient.listProfiles(adminId)`, loop the existing per-profile compute call, sum with nested per-member breakdown) is reused identically in every phase — it is built once, in Phase 1, and consumed by Phases 3-4 without re-design.
- Individual member dashboards (e.g., a standalone "Shweta's dashboard") are **not built**. Only the admin (Ketan) authenticates; per-member data is a client-side filter over the one family payload's nested `members[]` array, not a separate snapshot or a separate auth-gated view.
- `HEALTH_VITALS_SUMMARY` and `HOUSEHOLD_EVENT_SUMMARY` are **not** affected — those stay per-profile (vitals/events are inherently per-person, never summed). This scope change is wealth-domain/Epic-8 only.
- Q21's original A/B/C framing (does a joint account count toward both owners' individual net worth) is now moot — the Kotak account simply contributes once to the one family total, counted inside its designated owner's (Shweta's) member entry. ADR-016's designated-`profile_id`-of-record schema is unchanged and still required for this to work correctly.

---

## Decision 1 — Expense Tracking Granularity

**Recommendation: minimal hardcoded category set now, generalized rules engine later (the middle path).**

Five fixed categories, stored as a Java enum + OpenAPI enum on `transaction.metadata.category`:
`HOUSEHOLD_CORE`, `CHILD_RELATED`, `MAINTENANCE`, `DISCRETIONARY`, `UNCATEGORIZED`.

**Why not a single rolling total:**
Use Case 8.3 (Operating Budget Cap) and 8.5 (30-70 Target) both need a discretionary-vs-fixed split. A single total cannot answer "did I overspend on discretionary this month" — that's the whole point of the product owner's manual system. Losing per-category insight defeats the purpose of automating it.

**Why not the full Rule-Based Tagging Engine now (v1.3 scope):**
The v1.3 engine (`ROADMAP.md` "Rule-Based Tagging Engine" — admin UI, description-pattern rules like `SWIGGY → Food`) is a generalized, user-configurable system. Building it now pulls a v1.3 feature forward and adds an admin UI dependency Epic 8 does not need. Epic 8 only needs *a* category value per transaction to sum against — not a rule engine.

**Why hardcoded categories work today:**
- Category is a discriminator → VARCHAR, OpenAPI enum + Java enum, no SQL ENUM, no CHECK constraint (ADR-010 compliant).
- `wealth.transaction.metadata JSONB` already exists (V1 migration) — no schema change needed, only a new key: `metadata.category`.
- Category assignment for v1 is manual (set at upload time is too early — bank CSVs don't carry categories) — so it must be a **post-upload edit**, not parser-derived. This is a new requirement not in the original 8.1–8.6 text; see Phase 2 below.

**Tradeoff accepted:** the product owner must manually tag transactions into 1 of 5 buckets (no auto-tagging in Epic 8). This is acceptable because:
1. It unblocks 8.3 and 8.5 without building a rules engine.
2. The same `metadata.category` field is forward-compatible — when the v1.3 rules engine ships, it just writes the same field automatically; existing manually-tagged transactions are untouched.
3. `UNCATEGORIZED` is the default for all existing/un-tagged rows, so nothing breaks on rollout — uncategorized transactions are simply excluded from category-specific sums and flagged in the Use Case 8.4 validation report (consistent with the "missing growth-rate assumption" pattern already specified for 8.1).

**Architecturally sound because:** this is purely a `metadata JSONB` value + enum validation, identical in shape to the existing `account_type` / `vital_type` / `error_type` discriminator pattern already used three times in this codebase. Zero new tables, zero new ports beyond a new use case method to set the category.

---

## Decision 2 — Joint Account Ownership Model

**Recommendation: Option 1 — single designated `profile_id` owner + `metadata.joint_owners` array for display/attribution only.**

**The problem:** `wealth.account.profile_id` is a single nullable FK. ADR-006 requires every query scoped to one active `profile_id`. The Kotak joint account has two real owners (Ketan, Shweta).

**Why not the many-to-many `account_owner` join table:**
- Breaks ADR-006 at the schema level — "every query scoped to active `profile_id`" stops being a single predicate; every wealth query would need to become `account_id IN (SELECT account_id FROM account_owner WHERE profile_id = ?)`. This is a structural change to every repository method, every port interface, and ripples into `ProjectionCalculationEngine`'s per-profile snapshot model (`projections.dashboard_snapshot` is keyed `(profile_id, snapshot_key)` — a join table breaks that key's meaning).
- It is the option the product owner's own bullet list calls "bigger schema change, enables true multi-profile querying" — true multi-profile querying is not what Epic 8 needs. Epic 8 needs the account's transactions summed into *a* net worth and *a* set of category subtotals — not two parallel, independently-computed net worths sharing the same underlying transactions (that would double-count the joint account's balance across both Ketan's and Shweta's dashboards, directly violating the Use Case 8.1 "zero leakage / counted exactly once" rule).

**Why not a household-level ownership tier:**
- No `household` concept exists at the wealth-domain or profile-domain level today. `profile.admin` is the closest thing to a household anchor, but introducing a new ownership tier is a much larger structural change (new schema concept, new FK target, new ArchUnit cross-domain implications) to solve a problem that affects exactly one account today. YAGNI — defer until a second joint account or a real multi-tenant household need appears.

**Why Option 1 fits ADR-006 and hexagonal rules cleanly:**
- The account keeps exactly one `profile_id` — every existing query, the `TransactionRepository` interface, the projection engine's per-profile snapshot key, and the `profile_id`-filter-in-adapter rule (ADR-006) are all unchanged. Zero ripple into ports or domain layers.
- `metadata.joint_owners: [profile_id_ketan, profile_id_shweta]` is attribution-only — used by the UI to display "Joint: Ketan & Shweta" on the account card, and optionally by the validation report (Use Case 8.4) to label the account, but it is never used as a query predicate. This keeps `domain/` and `ports/` exactly as ignorant of multi-ownership as they are of single-ownership today — `profile_id` handling stays adapter-only, per ADR-006.
- Consistent with the existing precedent: `wealth.physical_asset` already has a nullable `profile_id` with the documented convention "NULL = owned by the admin profile." A joint account is the same shape of problem (one row, ambiguous singular ownership) solved the same way (pick a canonical owner, carry the nuance in metadata).

**Designated owner choice:** **RESOLVED — Shweta** is the `profile_id` of record for the Kotak account (confirmed by the product owner alongside Q21's resolution); Ketan is recorded in `metadata.joint_owners`. This is a one-time manual choice at account-creation time, same as setting `institution_name`.

**New ADR required:** ADR-016 (added to `documents/ARCHITECTURE_DECISIONS.md` — see below).

**Product-owner question — RESOLVED (Q21, 2026-06-30):** superseded by the household-rollup decision (ADR-017) — the dashboard's primary view is now one family total, not per-individual figures, so "does the joint account count toward Ketan's vs. Shweta's individual net worth" no longer applies. The account contributes once, inside Shweta's member entry, to the one family total.

---

## Phased Implementation Plan

### Phase 1 — Classification Foundation & Net Worth Correction

**Epic 8 use cases covered:** 8.1 (Dynamic Header Summation, Asset Categorization, Category Subtotal Reconciliation, Constraint Validation — partial), 8.6 (manual entry boundary for category/purpose/liquidity-tier fields)

**Bug fixes folded in:**
- **Bug 2 — Net worth formula wrong** (`ProjectionCalculationEngine.java` lines 72–74, 128–131). This phase is the categorization + summation engine. It is impossible to build "Total Gross Assets" and category subtotals correctly while `computeNetWorth()`/`computeTotalBalance()` still sum `opening_balance` only. The fix (new `GET /v1/accounts/{accountId}/balance` endpoint returning `opening_balance + SUM(CREDIT) - SUM(DEBIT)`) is a prerequisite, not an optional cleanup — Use Case 8.1's "Mahesh Summation Rule" is explicitly defined as summing ledger transactions, never a manually-entered or stale total. Do this first within Phase 1.
- **Bug 4 — `TransactionPanacheRepository` no `profile_id` filter** (`findByAccountId()` line 41, `existsByDeduplicationKey()` line 66). The new balance endpoint added for Bug 2 calls `findByAccountId()` to sum transactions. This is the first time that method gets a second caller path (today only `StatementUploadService` calls it). Adding a second caller without the `profile_id` filter doubles the exposure of the existing gap. Fix it here, before the second caller exists, not after.

**New schema/migration needed:** Yes.
- `wealth.account` gets a `metadata JSONB NOT NULL DEFAULT '{}'::jsonb` column (it does not exist today — only `transaction` and `physical_asset` have `metadata`). This is the carrier for: asset category, liquidity tier, purpose tag, joint-owner array (Decision 2), and all Use Case 8.6 manual-entry fields that apply to accounts.
- `wealth.physical_asset` already has `metadata JSONB` — no change needed there; category/liquidity/purpose tags for physical assets go in the existing column.
- New Flyway file `V6__account_metadata.sql` in `application/flyway/wealth/`. Follow V4's comment convention (explain why JSONB, not new columns — same sparse/extensible rationale already used for `transaction.metadata` and `physical_asset.metadata`).

**New parser work needed:** None. This phase touches no CSV parsing — it operates on existing `account` and `transaction` data plus newly-added manual metadata.

**What gets built:**
- `AccountUseCase` gains a method to set classification metadata (category, liquidity tier, purpose tag, joint owners) — a thin wrapper around the existing `PUT /accounts/{id}` metadata update path, per the Decision pattern already used for `physical_asset.metadata`.
- `ProjectionCalculationEngine` gains the category-summation and zero-leakage-count logic (every account/asset resolves to exactly one category; flag if not). This is the first piece of Use Case 8.4's validation engine, scoped narrowly to category resolution only — full pass/fail/warning persistence comes in Phase 4.

**New in this phase — Family Rollup Aggregation (added 2026-06-30, ADR-017):**
This is additive to the Bug 2 fix above, not a replacement. The per-account balance fix gives a correct *single-account* number; this step sums correct numbers across every member profile under one household.
- `ProjectionCalculationEngine` gains a small aggregation helper: resolve household members via `ProfileServiceClient.listProfiles(adminId, isActive=true)` (client already exists, no new gateway code needed for this call), then loop the existing per-profile `computeNetWorth()`-style call once per member, summing into a family total with each member's result retained as a nested entry.
- New `SnapshotKey` constant: `WEALTH_NET_WORTH_FAMILY`. UPSERT target is the admin's own SELF `profile_id` (same identifier already used everywhere — see ADR-017 for why not `admin.id`).
- Payload shape: `{ "family_total": ..., "members": [{ "profile_id", "name", "subtotal", "account_count" }, ...] }` — matches the product owner's `assets_06062026.json` reference shape.
- This same loop-and-sum helper is reused unchanged by Phase 3 (EMI/loan rollup) and Phase 4 (goals, validation) — built once here, not redesigned later.
- Existing singular `WEALTH_NET_WORTH` key/method are not deleted (kept for mechanical compatibility) but the dashboard's primary read path becomes the `_FAMILY` key once this ships.
- Liquidity tiering and purpose grouping are stored as metadata in this phase but not yet consumed by any computation (that starts Phase 3).

---

### Phase 2 — Statement Source Expansion & Expense Categorization

**Epic 8 use cases covered:** 8.1 (Liquidity Tiering — data plumbing only, no computation yet), Decision 1's category field (new requirement, not in original 8.1–8.6 text — manual transaction tagging)

**Bug fixes folded in:**
- **Bug 1 — Gateway `/errors` proxy missing** (`WealthGatewayResource.java`, `WealthServiceClient.java`). This phase adds statement ingestion for new sources (Bank of India car loan, salary/savings bank, Kotak joint account, credit card). New statement sources mean a higher volume and higher variety of CSV parse failures during onboarding (new column layouts hitting `StatementCsvParser` for the first time). The error log and its gateway-proxied retrieval becomes load-bearing the moment the product owner starts uploading these new files — fix the proxy gap before, not after, multiple new bank formats are thrown at the parser.

**New schema/migration needed:** Yes.
- `wealth.transaction.metadata` already exists — add the `category` key (no DDL, just a new JSONB key per Decision 1). No migration required for this alone.
- If Bank of India / Bank of Baroda specific fields are needed beyond what `metadata` already supports (e.g., MaxGain offset linkage — see Phase 3), that schema work is deferred to Phase 3 where it is actually consumed.

**New parser work needed:** This is the phase that answers the "does `StatementCsvParser` need to become format-aware" question.

**Recommendation: stay generic, do not build a per-bank column-mapping registry.**
`StatementCsvParser.detectColumns()` already does header-name matching across a candidate list per logical column (date, description, debit, credit, amount) — see `DATE_HEADERS`, `DEBIT_HEADERS`, etc. This is already bank-agnostic by design, not hardcoded to one layout. The pragmatic path for each new source (Bank of Baroda, Bank of India, salary bank, Kotak, credit card, MF platform) is: **extend the existing candidate-header lists** with each bank's actual column names (e.g., add Kotak's specific header strings to `DEBIT_HEADERS`/`CREDIT_HEADERS` if they differ from what's covered). Only fall back to a per-bank-format strategy class if a real statement is encountered that the candidate-list approach cannot parse (e.g., multi-row headers, non-tabular PDF-exported CSV). Do not pre-build format-aware infrastructure speculatively — YAGNI until a concrete file breaks the generic parser.
- Credit card statements: likely need a new `DEBIT_HEADERS`/`CREDIT_HEADERS` entries (e.g., "Transaction Amount", purchase vs. payment framing reversed from a bank account). Validate against a real Kotak/credit-card sample file before coding.
- Mutual fund/investment platform statements: already scoped (Epic 6.1, v0.3, delivered) — Units + NAV in `metadata`. No new work here, just confirm existing parser handles the specific platform's column names.
- Loan statements (Bank of Baroda MaxGain, Bank of India car loan): these likely report EMI debits only, no separate principal/interest split column — confirms Use Case 8.2's requirement that the split must be *derived*, not parsed, which is exactly what Phase 3 builds.

**What gets built:**
- Extend `StatementCsvParser`'s header candidate lists per real statement sample (one PR per new source, each independently testable via `StatementCsvParserTest`).
- New use case method (or extend existing transaction update path) to set `metadata.category` on an individual transaction — manual tagging UI/endpoint. This is net-new scope beyond the original 8.1–8.6 text, required by Decision 1.
- Joint account creation: apply Decision 2 — Kotak account created with designated `profile_id` + `metadata.joint_owners`.

---

### Phase 3 — Loan Amortization, EMI Arbitrage & Liquidity Computation

**Epic 8 use cases covered:** 8.1 (Liquidity Tiering — full computation, Growth Projection), 8.2 (all sub-bullets — EMI split, outstanding balance, tenure tracking, offset arbitrage, prepayment-vs-invest, safety net validation)

**Bug fixes folded in:** None new. Bugs 1, 2, 4 are already fixed by Phase 1/2. No code this phase touches has a confirmed bug from the QA review.

**New schema/migration needed:** Yes.
- Loan-specific metadata keys on `wealth.account.metadata` (added in Phase 1's migration, populated now): original principal, loan start date, original tenure months, interest rate (already partially covered by existing `interest_rate` column from V4 — confirm whether to reuse that typed column or move to metadata for consistency; recommend **keep `interest_rate` as the existing typed column**, add only `original_principal`, `loan_start_date`, `original_tenure_months`, `linked_offset_account_id` to `metadata`, since `interest_rate` already has a dedicated NUMERIC column from V4 and duplicating it in JSONB would violate single-source-of-truth).
- No new migration file needed beyond Phase 1's `V6__account_metadata.sql` if that migration's column is generic JSONB — these are just new keys written into the same column.

**New parser work needed:** None directly — this phase computes from already-ingested transaction data (Phase 2) plus manually-entered loan metadata (Use Case 8.6 boundary). Confirms loan-related parser work was front-loaded into Phase 2.

**What gets built:**
- Amortization calculator (pure function, lives in `domain/` — no framework deps, easily unit-testable with plain `new`): given principal/rate/tenure/elapsed-EMI-count, derive principal/interest split per period, outstanding balance, remaining tenure.
- Offset/MaxGain arbitrage calculation in `ProjectionCalculationEngine` (or a new gateway-side calculator class, consistent with ADR-013's pattern of one method per metric).
- Liquidity tier computation, consuming the tier metadata set in Phase 1.
- Growth projection (5yr/10yr) — flags accounts missing the growth-rate assumption per Use Case 8.1, feeding into Phase 4's validation report.
- **Family rollup (ADR-017):** EMI/loan figures are aggregated the same way net worth was in Phase 1 — reuse the Phase 1 loop-and-sum helper (`listProfiles(adminId)` + per-member compute + sum-with-nested-breakdown), new key `WEALTH_EMI_TRACKING_FAMILY`. No new aggregation logic is written here; this phase only adds the per-member compute method the helper loops over.

---

### Phase 4 — Goals Engine & Validation Gate

**Epic 8 use cases covered:** 8.3 (Dynamic Triggers, Operating Budget Cap, SIP Protection), 8.4 (full validation/zero-leakage rule engine, persisted PASS/WARNING/CRITICAL result set), 8.5 (five-type goals engine)

**Bug fixes folded in:**
- **Bug 3 — `refreshAll()` no per-step exception isolation** (`ProjectionCalculationEngine.java` lines 54–59). Use Case 8.4 explicitly requires "Non-Blocking by Default" — a CRITICAL FAILURE in the new validation engine must not prevent unrelated snapshot keys (vitals, events) from refreshing. This is the exact same defect as Bug 3, just now with a concrete new caller (the validation engine) that depends on the isolation actually working as documented. The Javadoc at line 50 already claims this behavior; Phase 4 is where the claim becomes load-bearing for a new feature, so the implementation must finally match the comment. Fix here, wrapping each of the (now five+) compute steps including the new validation step in its own try-catch.

**New schema/migration needed:** Yes.
- New `projections` schema additions: validation result payload — new snapshot key `WEALTH_VALIDATION_REPORT_FAMILY` (household-scoped per ADR-017, not per-profile), following the existing UPSERT-on-`(profile_id, snapshot_key)` pattern, keyed by the admin's SELF profile_id — no new table required, reuses `projections.dashboard_snapshot`.
- Goals engine output: also a new snapshot key, `WEALTH_FORMULA_GOALS_FAMILY` — the five formula-driven goals (Debt Crossover, 30-70 Target, Freedom Runway, Insurance Free, Year One) are household-level by definition (one combined debt figure, one combined EMI total, per the product owner's `Financial_Data.md`), so there is no per-member variant to aggregate — this key is naturally family-scoped from day one, distinct from the existing `WEALTH_GOAL_PROGRESS` key tied to `household.goal` (which stays per-profile/per-user-created-goal). Per Q18 (existing open question), these are two separate goal systems — no schema merge.
- Policy/threshold storage for goal targets, budget cap, expected-market-return-rate: these are one-time manual values (Use Case 8.6), and per the household-rollup decision they are household-level policy, not per-profile. Recommend a small `wealth.policy_setting` key-value table scoped by `admin_id` (not `profile_id`) rather than overloading `account.metadata`. This is a new table, flagged for architect schema review at Phase 4 planning, not designed in full here.

**New parser work needed:** None.

**Family rollup note (ADR-017):** the validation engine and goals engine both reuse the Phase 1 aggregation helper. Validation runs its category-resolution and double-counting checks once across the combined household ledger (all members' accounts/transactions), not once per profile — this is actually simpler than a per-profile validation pass would have been, since "zero leakage" is naturally a household-wide property (the same transaction must not be double-counted across two members' totals, which the single-rollup-pass design prevents by construction).

**What gets built:**
- Five hardcoded goal formula calculators (Decision per Q14 — pending product owner answer, default assumption: hardcode, per the BA's option A/C recommendation).
- Validation rule engine: category resolution check, double-counting check, missing-growth-rate-assumption check, monthly cashflow check — each producing PASS/WARNING/CRITICAL, persisted to the snapshot payload.
- Reallocation trigger simulation and SIP protection check (advisory-only per Use Case 8.4's non-blocking default, pending Q16 confirmation).

---

## Sequencing Rationale

1. **Phase 1 before everything**: nothing in 8.1–8.5 is trustworthy while net worth sums `opening_balance` only. Fixing Bug 2 here is not optional cleanup — it is the literal definition of "Dynamic Header Summation" in Use Case 8.1. Category/liquidity/purpose metadata fields must exist on `account` before any later phase can read them, so the schema addition is also front-loaded here. The family-rollup aggregation helper (ADR-017) is also built in Phase 1, immediately on top of the corrected per-account balance — correct per-account numbers must exist before they can be correctly summed across household members, and every later phase's family-scoped output (EMI, goals, validation) reuses this same helper rather than rebuilding it.

2. **Phase 2 before Phase 3**: loan and expense data must be ingested (new statement sources, expanded parser coverage) before EMI arbitrage (Phase 3) or budget-cap/category checks (Phase 4) have any real data to compute against. This directly answers the prompt's question about why parser flexibility planning affects 8.1/8.2 sequencing — a derived EMI split is meaningless with zero loan transactions in the ledger.

3. **Phase 3 before Phase 4**: the goals engine (Debt Crossover, Freedom Runway) and the validation engine both consume liquidity tiers and outstanding-loan-balance figures computed in Phase 3. Building the goals engine first would mean computing against placeholder/zero values.

4. **Bug fixes are pulled forward only when the phase's new code creates or depends on the exact failure mode** — never bundled as a blanket "fix bugs first" phase, per the product owner's explicit sequencing instruction (Decision 2 in the task brief). Bug 1 lands in Phase 2 (new upload volume makes the gap acute), not Phase 1, because Phase 1 does no CSV ingestion. Bug 3 lands in Phase 4, not earlier, because no earlier phase adds a new `refreshAll()` step whose isolation is actually being relied upon — Phases 1–3 modify what the existing four steps compute, not how many steps exist or how they fail independently.

5. **Decision 2 (joint account) is resolved in Phase 1/2**: the schema (Phase 1's `account.metadata`) must exist before the Kotak account can be created with `joint_owners` (Phase 2, when new statement sources are onboarded).

---

## Open Items Requiring Product Owner Decision

These are net-new items surfaced by this plan, continuing the numbering from `OpenQuestions.md` Q1–Q20. They are genuine policy/product tradeoffs, not schema questions — the schema questions (Decision 2's account ownership model) are resolved above without escalation.

*Q21-Q25 are tracked authoritatively in `documents/OpenQuestions.md` — entries below are kept for historical context only; do not edit resolutions here, edit `OpenQuestions.md`.*

**Q21.** ~~Does a joint account's transaction activity count toward both owners' individual net worth figures...~~ **RESOLVED — 2026-06-30.** Superseded: the dashboard's primary view is a household rollup (ADR-017), so the Kotak joint account simply contributes once to the one family total inside its designated owner's (Shweta's) member entry — the original A/B/C individual-vs-joint framing is moot. See `OpenQuestions.md` Q21 and Q25, and `ARCHITECTURE_DECISIONS.md` ADR-017.

**Q22.** Should the five hardcoded expense categories (`HOUSEHOLD_CORE`, `CHILD_RELATED`, `MAINTENANCE`, `DISCRETIONARY`, `UNCATEGORIZED`) match the product owner's existing reference MD file's category names exactly, or is the architect's naming in Decision 1 acceptable?
*Context:* Decision 1 proposes category names based on the task brief's summary of the reference file ("Household Core, Child-Related, Maintenance, Discretionary"). If the actual reference file uses different category boundaries or additional categories (e.g., separate "Insurance" or "Travel" buckets), the enum should match before Phase 2 ships, since changing an enum discriminator later means a data-migration pass over already-tagged transactions (not a schema migration, since VARCHAR has no CHECK constraint — but still a re-tagging effort).
*Options:*
A) Confirm the four named categories + `UNCATEGORIZED` exactly as proposed.
B) Provide the exact category list from the reference MD file for the architect to align the Java enum before Phase 2 implementation starts.

**Q23.** For Phase 4's policy/threshold values (goal targets, budget cap, expected-market-return-rate), is a new `wealth.policy_setting` key-value table the right home, or should these be modeled as a richer typed table per policy type?
*Context:* This plan flags the table as a Phase 4 architecture decision, not fully designed here per the task's "no full DDL" instruction. A key-value table is the YAGNI-consistent choice (matches the JSONB-for-sparse-data philosophy used everywhere else in this domain), but if the number of distinct policy types grows significantly beyond the ~7 listed in Use Case 8.6, a typed table per policy category may be cleaner. This is an architect call at Phase 4 planning time, not a product owner call — listed here only so it is not forgotten, and flagged for re-review when Phase 4 starts (net worth formula and loan metadata will be live by then, giving real data to size the decision against).
*Options:* N/A — this is a placeholder for re-review at Phase 4 start, not requiring a product owner answer now.

**Q24.** Should Phase 2's manual transaction-category tagging (Decision 1) reuse the existing `PUT`-style update path, or does it need a dedicated bulk-tagging endpoint given that a 12-month statement history could mean hundreds of untagged transactions at once?
*Context:* Use Case 8.6 frames classification as "one-time manual classification per account" but category is requested by Decision 1 at the *transaction* level, not the account level — meaning potentially hundreds of rows need tagging after the first bulk CSV upload, not a one-time setup per account. A single-row `PUT /transactions/{id}` endpoint works but is tedious for bulk history; a bulk-tag endpoint (e.g., "tag all transactions matching description pattern X" or "tag all transactions in date range Y") is closer to the v1.3 Rule-Based Tagging Engine's shape, which Decision 1 explicitly deferred. Building a bulk endpoint now risks scope creep into v1.3; not building one risks an unusable UX for a 12-month statement backlog.
*Options:*
A) Single-row tagging only in Phase 2 (`PUT /transactions/{id}` metadata update) — accept the bulk-tagging UX gap until v1.3's rules engine ships; the product owner manually tags as needed, starting with current/recent transactions only.
B) Add a minimal bulk-tag-by-selection endpoint in Phase 2 (tag a list of transaction IDs in one call) — smaller than a rules engine, but removes the worst of the one-row-at-a-time friction for initial backlog tagging.

---

## Document Cross-References

- `documents/REQUIREMENTS_wealth_domain.md` — Epic 8, Use Cases 8.1–8.6 (authoritative scope)
- `documents/ROADMAP.md` — Architect/BA/QA reviews (2026-06-29, 2026-06-30) — bug locations, milestone placement recommendation
- `documents/OpenQuestions.md` — Q1–Q20 (existing), Q21–Q25 (Epic 8 planning; Q21 and Q25 resolved 2026-06-30 — household rollup)
- `documents/ARCHITECTURE_DECISIONS.md` — ADR-016 (joint account ownership, Decision 2), ADR-017 (household-level dashboard aggregation — household rollup mechanism, supersedes Q21's original framing)
- `documents/domain-state/wealth.md` — current schema, key files, backlog, household-rollup decision (ADR-017)
- `documents/domain-state/household.md` — `household.goal` vs. wealth goals-engine relationship (Q18), projection engine notes
