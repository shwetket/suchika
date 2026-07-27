# Open Questions

| | |
|---|---|
| **Type** | Decision Log |
| **Audience** | Product owner (Ketan) |
| **Status** | Active |
| **Last updated** | 2026-07-12 (Q55–Q64 added — migrated in from `ROADMAP.md`'s embedded "Open Questions for the Product Owner" sections, which duplicated this file's purpose; Q49–Q53 pointer corrected — `CONTRACT_CONSOLIDATION.md` plan is DONE, not still-unexecuted) |

## Purpose

Single shared log for all open questions raised during reviews. Answer each question here so agents can proceed. Questions are numbered for easy reference — continue numbering from **Q65** for any new question raised (Q64 is already in use, see below).

---

## Open Questions

*Raised by the 2026-07-06 pre-v1.0 Architect Review and Business Analyst Review (`ROADMAP.md`) — moved here 2026-07-12 since that's this file's job, not ROADMAP's. All still genuinely open as of this migration.*

**Q55.** Consolidate the five independently-deployed Quarkus services into a modular monolith before starting v1.0 auth/encryption work?
*Context:* The architect's retrospective flagged that 5 services for a single-household, no-auth, one-operator local app is the single biggest thing worth reconsidering, and now (before v1.0 multiplies the auth/encryption surface by 5) is the cheapest point to do it. Concrete costs already visible: the contract-mirror drift bug class, the now-fixed `/errors` gateway-bypass bug (structurally impossible in a monolith), and the 1200+-line `ProjectionCalculationEngine` (needs per-step try/catch specifically because cross-service REST calls fail independently).
*Options:*
A) Consolidate to a modular monolith now, before v1.0.
B) Keep 5 services, but centralize OIDC validation at the gateway only so domain services never independently authenticate.
C) Keep the current architecture as-is into v1.0.

**Q56.** Adopt a "decided vs. done" status distinction for `OpenQuestions.md`-style decision logs going forward?
*Context:* Multiple "RESOLVED" decisions (Testcontainers adoption, E2E-in-CI, contract tests, RestAssured standardization — see `ROADMAP.md`'s tracked debt list) were approved months ago and never implemented, with nothing flagging the gap until this retrospective found it by re-checking. A resolved-but-unimplemented decision currently looks identical to a resolved-and-shipped one in this file's format.

**Q57.** Is Epic 8 (wealth financial engine) "done, don't touch," or should more of its hardcoded values move into `profile.admin.policy_settings`?
*Context:* 5 of 6 Epic 8 use cases shipped (8.3 — dynamic reallocation triggers, budget-cap alerts, SIP-gap checks — was deliberately not built; see `REQUIREMENTS_wealth_domain.md`). If formulas/thresholds keep needing adjustment as real usage accumulates, more of what's currently hardcoded in `ProjectionCalculationEngine` could move into the existing `policy_settings` JSONB column instead of requiring a code change each time.

**Q58.** Formally close PROP-002 (Cross-Domain Data in Restricted Profiles) and PROP-003 (Event Sourcing for Wealth) now?
*Context:* Both still show "Open" in `documents/ARCHITECTURE_PROPOSALS.md`. The architect and business-analyst both recommend closing them: PROP-002 because no auth/roles exist yet, so carrying an open proposal about a not-yet-built permission system is pure overhead (recommend Option A — block entirely — over redaction, given zero real usage data to design redaction around); PROP-003 as rejected outright, since nothing in 4 shipped versions needed event sourcing and the existing CQRS snapshot pattern already gives most of the value it was chasing.

**Q59.** `shared.yaml`'s `Error` schema doesn't match `ErrorResponse.java`'s actual runtime shape (see `LOGGING_AND_EXCEPTIONS.md` for the real shape). Fix the contract to describe reality (cheap), or use this moment to adopt the structured `details[]` array the contract already promises (a real DTO change)?

**Q60.** RBAC role source (for v1.0 auth): derive Admin/Restricted from the existing `relation_to_admin` field, or add a new independent role field?
*Context:* `relation_to_admin` has 9 values (`SELF`, `SPOUSE`, `CHILD`, `PARENT`, `PARENT_IN_LAW`, `SIBLING`, `GRANDPARENT`, `GRANDCHILD`, `OTHER`). If roles derive from it, how do all 9 map onto a 2-tier Admin/Restricted split?

**Q61.** Encryption scope for v1.0: peripheral/identifying fields only (narration, doctor/hospital names, registration numbers), or full ledger values (`amount`, `txn_date`)?
*Context:* The fields that make a financial ledger sensitive are exactly what Epic 8 depends on as plaintext SQL predicates (SUM aggregation, dedup key, date filters) — encrypting them means moving that arithmetic out of SQL into the application layer, materially bigger than "add a crypto utility." The business-analyst recommends peripheral-only for v1.0 and deferring full ledger encryption. Needs deciding before any code is written.

**Q62.** Does Google Fit (manual sync) need to ship alongside auth/encryption/persistence in v1.0, or can it be sequenced separately?
*Context:* It's a second, independent OAuth relationship (Suchika ↔ Google) distinct from the app's own login OAuth. The business-analyst recommends cutting it from the v1.0 batch — it's the one v1.0 item that's purely additive rather than closing a security/durability gap.

**Q63.** Confirm Testcontainers adoption as a hard-blocking prerequisite of "Persistent Data Migration" (v1.0), not ambient cleanup?
*Context:* Independently confirmed unimplemented across profile/wealth/health/household (all adapter DB tests still hit the shared local dev Postgres via a `%integration-test` profile — see the Q34/Q35 pointer in the archive section below). The whole point of "Persistent Data Migration" is that the DB now holds real data; the current test suite is a confirmed, repeated threat to exactly that data.

**Q64.** Implement the guard, or fix the contract: `DELETE /v1/profiles/{profile_id}` promises a `409 FailedPrecondition` when deactivating the SELF profile of an active admin, but `ProfileService.deactivateProfile`/`updateProfile` have no such guard — two green tests (`ProfileServiceTest`) explicitly construct this exact case and assert deactivation *succeeds*.
*Context:* Found in the 2026-07-06 business-analyst retrospective; still open per `documents/domain-state/profile.md` Open Issues. Two of this same retrospective's other findings (household's `NOT NULL` restoration, the dead `profile.profile.metadata` column) were already fixed in the 2026-07-08 v0.5.1 pass — this one wasn't.
*Options:*
A) Implement the guard, mirroring `AdminService`'s existing `countActiveByAdminId` check.
B) Remove the false 409 guarantee from `application/contract/profile.yaml` instead.

---

<!-- Architect, Business Analyst, and QA agents append questions below. Keep numbering sequential, starting at Q65. -->

## Archive — Where Q1–Q54's Answers Live Now

Every question previously logged here (Q1–Q54, spanning the 2026-06-29 architect/BA/QA reviews through the 2026-07-05 contract-consolidation reconciliation) was resolved. Rather than keep the full original log — which duplicated content already recorded at its authoritative source — each resolution has been verified to already live in one of these places:

| Where | What it covers |
|---|---|
| `documents/ARCHITECTURE_DECISIONS.md` | ADR-016 through ADR-022 — joint accounts, household rollup, React Query, `profileId`-as-domain-field, Flyway/DB-constraint policy, login auto-attach, goal plans |
| `CLAUDE.md` | DB constraint philosophy (FK/UNIQUE kept, CHECK dropped), `VARCHAR(50)` name-column cap, Flyway edit-in-place exceptions |
| `documents/domain-state/{profile,wealth,health,household}.md` | Per-domain implementation of every resolved feature/behavior decision (inventory lifecycle, manual transaction entry, vitals edit, category tagging, goal formulas, income auto-detection, etc.) |
| `documents/ROADMAP.md` | Coverage-floor decision (Q9) and its still-not-wired-into-Gradle status; the "decided but never implemented" gaps (Testcontainers — Q34/Q35; E2E-in-CI — Q10; contract tests — Q11; RestAssured standardization — Q38) tracked as ongoing debt; the pagination-shape decision (Q54, resolved 2026-07-07, superseding the version of Q54 that had been logged here) |
| `documents/CONTRACT_CONSOLIDATION.md` | Q49–Q53 in full (shared OpenAPI components — error responses, `profile_id` param variants, pagination) — **DONE, verified 2026-07-12** (`shared.yaml` adopted by all 5 contracts); file renamed from `contract-consolidation-plan.md` and kept as a historical record, not archived here |
| `documents/UX_DECISIONS.md` | UX-numbered decisions from later reviews (separate numbering series, unaffected by this cleanup) |

If you need the original question text/options for archaeology, it's in git history for this file prior to 2026-07-12.

---

## Template for New Questions

```
**Q65.** <question>
*Context:* <why this needs a decision, what's blocked on it>
*Options:*
A) <option>
B) <option>
```

When answered, replace the entry with:

```
**Q65.** ~~<original question, truncated>...~~ **RESOLVED — <date> (<who>).**
*Resolution:* <the decision>
Then move the resolution + rationale into the respective authoritative doc (ADR, domain-state file, ROADMAP, or the relevant plan doc) and remove the entry from this file — this file should only ever contain genuinely open questions.
```
