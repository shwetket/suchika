# Master Business Requirements: Personal Operations System (Suchika)

| | |
|---|---|
| **Type** | Requirements |
| **Audience** | All developers, product |
| **Status** | Active |
| **Last updated** | 2026-07-12 (milestone status table updated — was frozen at v0.2-era "PLANNED" for v0.3–v0.6, which have all since shipped) |

## Objective

Define the overarching business goals, core principles, and milestone roadmap for Suchika. This is the single source of truth for *what* the system must do and *why* — it does not prescribe *how*. Detailed acceptance criteria per domain live in the child requirement documents (see section 4).

## Use Cases

- Before scoping a new feature — check which version milestone it belongs to
- When evaluating whether a proposal crosses domain boundaries prematurely (cross-domain logic is banned before v0.5)
- When writing acceptance criteria — use the principle definitions in section 2 as the guardrails

---

## 1. Executive Summary

This document defines the overarching business requirements, architecture principles, and maturity milestones for the Suchika personal operations application. The system is designed to consolidate Wealth, Household, and Health data into a single, strictly organized repository. The ultimate long-term goal (v2.0+) is to enable a local Artificial Intelligence to seamlessly read, analyze, and automate personal operations based on highly structured, immutable data.

**Status as of 2026-07-12:** v0.6 is complete. All four domains (Profile, Wealth, Health, Household) are fully implemented, including Epic 8 (wealth financial engine, 5 of 6 use cases) and its ADR-022 follow-on (goal plan/insurance policy management). v0.7 is not yet planned; v1.0 (Security & Persistence) is next after any v0.7 gap-filling — see `ROADMAP.md`.

---

## 2. Core Principles & Assumptions

The following principles dictate all technical and business decisions regarding this system:

* **Ephemeral Test Data (Pre-v1.0):** Until the v1.0 (Security & Persistence) milestone is reached, all local database records are treated as volatile test data. The system is not required to handle complex data migrations or prevent corruption via edge cases early on, as the database will be wiped and reset frequently during local development.
* **AI Readiness via Data Hygiene:** Data quality is critical. Information must be strictly structured and categorized from Day 1 to ensure seamless ingestion by a future local LLM.
* **Strict Domain Isolation:** Data must be logically separated into distinct domains (Wealth, Household, Health) to prevent cross-contamination and ensure system stability.
* **Calculated Risk & Efficiency:** Core technical capabilities must be proven early with minimal scope (e.g., "Happy Path" in v0.1).
* **Declarative Documentation:** Acceptance Criteria are written as clear, declarative statements optimized for human readability, rather than strict BDD (Given/When/Then) syntax.

---

## 3. Versioning Approach & Roadmap

The application follows a granular maturity roadmap. Core features are introduced in early stages, with error handling, security, and external integrations strictly deferred to later milestones. For the full milestone breakdown with shipped vs. planned features, see [ROADMAP.md](./ROADMAP.md).

| Version | Stage | Key Focus | Status |
| :--- | :--- | :--- | :--- |
| **v0.1** | Prototype | Minimal core capabilities, happy path execution only. | DONE |
| **v0.2** | Usable Local App | Profile, Wealth, and Health domains fully usable. UAT-ready pilot. | DONE — UAT |
| **v0.3** | Enhanced Local App | Household domain, dashboard live data, conflict detection. | DONE |
| **v0.4** | Error Handling + Epic 8 | Malformed data rejection; Epic 8 wealth financial engine (5 of 6 use cases — see `REQUIREMENTS_wealth_domain.md`). | DONE |
| **v0.5** | Beta Release | First cross-domain logic — Vacation Planner, Consolidated Action Center. React Query adopted (ADR-018). | DONE |
| **v0.6** | Testing Foundation | Automated test coverage — new ArchUnit port-coverage rule, Jest branch coverage gate, transaction/list pagination. | DONE |
| **v0.7** | *(not yet planned)* | Gap-filling before v1.0, if any — see `ROADMAP.md`. | NOT SCHEDULED |
| **v1.0** | Security & Persistence | Authentication, encryption, and transition to persistent, real data. | PLANNED — next |
| **v1.1** | Multi-User | User accounts and role-based access. | PLANNED |
| **v1.2** | Public Local Release | Stable local release for general users. | PLANNED |
| **v1.3** | Export / Import | Advanced data framework management. | PLANNED |
| **v2.0** | Local AI | Integration of AI-powered features and data synthesis. | PLANNED |
| **v2.1** | Cloud Ready | Architectural preparation for cloud deployment. | PLANNED |
| **v2.2** | Mobile App | Development of a companion mobile application. | PLANNED |
| **v3.0** | GitHub Ready | Open-source collaboration readiness. | PLANNED |
| **v3.1** | Integrations | Google Drive, Calendar, Fitbit, etc. | PLANNED |
| **v3.2** | Plugin Framework | System extensibility. | PLANNED |
| **v3.3** | Marketplace | Development of a plugin/module ecosystem. | PLANNED |
| **v4.0** | Cloud Launch | Full commercial cloud deployment. | PLANNED |
| **v4.1** | Commercial Launch | Licensing, regulatory compliance, and billing. | PLANNED |

---

## 4. Domain Definitions & Document Hierarchy

Detailed business rules, epics, and version-specific Acceptance Criteria are maintained in domain-specific child documents. These files act as living documents that represent the accumulative state of the system.

* **Profile (Identity Anchor):** Every other domain's records carry a `profile_id` foreign key referencing `profile.profile`. All data is member-scoped — queries are always filtered by the active `profile_id`. Profile is a prerequisite for all other domains.
* **Wealth & Asset Management:** [REQUIREMENTS_wealth_domain.md](./REQUIREMENTS_wealth_domain.md)
  * *Focus:* Financial liquidity, transaction ledgers, and physical asset lifecycle compliance.
* **Household Operations:** [REQUIREMENTS_household_domain.md](./REQUIREMENTS_household_domain.md)
  * *Focus:* Scheduling, human logistics, task execution, supply chain (groceries), and home infrastructure automation.
  * *v0.2 status:* Deferred to v0.3. No Household features are part of the v0.2 UAT scope.
* **Health & Biometrics:** [REQUIREMENTS_health_domain.md](./REQUIREMENTS_health_domain.md)
  * *Focus:* Time-series biometric tracking and medical visit records.
* **Cross-Domain Logic:** [REQUIREMENTS_cross_domain.md](./REQUIREMENTS_cross_domain.md)
  * *Focus:* Features requiring read-access across multiple isolated domains (e.g., Vacation Planning requiring Calendar, Vehicle, and Finance data). No cross-domain features before v0.5.

---

## 5. Universal Business Rule — Member-Scoped Data

All data records across all domains (Wealth, Health, Household) are owned by a household member identified by `profile_id`. This is a structural business invariant enforced from v0.1 onward:

* Every create operation must receive a valid `profile_id` referencing an active member in `profile.profile`.
* Every list and view operation must filter by the requesting `profile_id`.
* No domain may return records belonging to a different member, regardless of request parameters.
