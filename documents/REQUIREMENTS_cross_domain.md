# Cross-Domain Integration

| | |
|---|---|
| **Type** | Requirements |
| **Audience** | Developers, product |
| **Status** | Active |
| **Last updated** | 2026-07-12 (Vacation Planner + Action Center marked DONE — both delivered 2026-07-02) |

## Objective

Define requirements for features that span more than one domain — composite views, unified dashboards, and AI synthesis. Cross-domain logic is strictly prohibited before v0.5; this document records what is planned and why the isolation constraint exists.

## Use Cases

- Before proposing any feature that reads from two or more domains — check whether it is permitted in the current milestone
- When designing the web-gateway BFF aggregation logic for dashboard features (v0.5+)
- When evaluating whether a new requirement is truly cross-domain or can be self-contained within a single domain

---

**Focus:** Inter-domain data querying, composite feature logic, unified dashboards, and AI data synthesis.

## Universal Rule: Member-Scoped Data Isolation (v0.1 onward) [DONE in v0.2]

Every domain (Wealth, Health, Household) enforces `profile_id`-scoped data isolation. This is not a cross-domain feature — it is a structural invariant present in each domain independently. All data records carry a `profile_id` foreign key referencing `profile.profile`. All list, view, create, update, and delete operations are scoped to the active `profile_id`. This rule was fully delivered as part of v0.2.

## v0.1 to v0.4: Domain Isolation Strategy
**Objective:** Stabilize core technical capabilities independently.
**Constraint:** Cross-domain logic is strictly prohibited in these early versions. Each domain (Wealth, Household, Health) must operate entirely isolated from one another to ensure foundational architectures solidify without inter-dependency bugs.

## v0.5: Beta Release (Stable for Testers) [DONE — 2026-07-02]
**Objective:** Introduce read-only composite views that aggregate data from isolated domains without breaking architectural boundaries.

### Epic 1: The "Vacation Planner" [DONE]
**Domains Required:** Household (Calendar) + Wealth (Finance & Vehicle Assets)
#### Use Case 1.1: Trip Feasibility & Asset Readiness [DONE]
* **Budget Validation:** When a user schedules a multi-day trip event in the Household calendar and inputs an estimated cost, the system must be able to read the current liquid savings balance from the Wealth domain to display a simple "Feasible / Not Feasible" status flag. [DONE]
* **Asset Compliance Block:** When scheduling a road trip event, the system must query the Vehicle compliance ledger (Wealth domain) and generate a high-priority warning if the vehicle's PUC, Insurance, or BH-Series Road Tax expires before or during the scheduled trip dates. [DONE]

**Implementation note (added 2026-07-12):** delivered as a gateway-native feature (`com.suchika.gateway.vacationplanner`), not household or wealth domain code — it depends only on `WealthServiceClient` (reads liquid savings + physical-asset compliance data). Lives under `/household/vacation-planner` in the nav for UX grouping only, not a backend boundary. One known, unbuilt integration: trip dates are manually re-entered even though `household.calendar_event` already models `EventType.TRAVEL` — not a bug, just an unexploited opportunity. See `documents/domain-state/wealth.md` and `household.md`.

### Epic 2: Consolidated "Action Center" [DONE]
**Domains Required:** Wealth + Household + Health
#### Use Case 2.1: Unified Dashboard Alerts [DONE]
* **Aggregation:** The system must feature a single, read-only dashboard that pulls urgent pending items across all domains. [DONE]
* **Notification Scope:** This dashboard must aggregate upcoming calendar events (Household), pending vehicle compliance renewals or recurring financial payments (Wealth), and missing biometric data logging streaks (Health). [DONE]

**Implementation note (added 2026-07-12):** `ProjectionCalculationEngine.computeActionCenterAlerts()` → `ACTION_CENTER_ALERTS_FAMILY` snapshot key, one of 13 compute steps in the CQRS projection engine (see `ARCHITECTURE.md`). Biometric gap threshold and tracked vital types resolved as their own product decision — see `documents/domain-state/health.md`.

## v1.0: Security & Persistence
**Objective:** Ensure that connecting multiple domains does not bypass the role-based access controls established in v1.0.

### Epic 3: Multi-Tenant Boundary Enforcement
#### Use Case 3.1: Profile Scoping Across Domains
* **Access Filtering:** Any cross-domain query must strictly respect the active user's household profile role.
* **Data Masking:** If a restricted profile (e.g., a Child) accesses the Household calendar, the system must block the calendar from executing any cross-domain queries to the Wealth domain (e.g., hiding the budget validation status for a family trip).

## v1.3: Export / Import
**Objective:** Allow users to download a comprehensive snapshot of their entire life operations.

### Epic 4: Global Data Archiving
#### Use Case 4.1: Cross-Domain Data Export
* **Unified Export:** The system must provide a single execution trigger that queries all five PostgreSQL schemas (profile, wealth, health, household, projections) and packages all user data into a standardized, structured local backup format (e.g., JSON/CSV archives).

## v2.0: Local AI (Long-Term Vision)
**Objective:** Enable a local Large Language Model to act as a unified reasoning engine over personal data.

### Epic 5: AI Context Aggregation
#### Use Case 5.1: Cross-Domain Synthesis
* **Read-Only Context API:** The system must provide a unified, strictly structured API layer that allows the local AI agent to simultaneously query Wealth ledgers, Household schedules, and Health biometrics.
* **Daily Briefing Generation:** The system must ensure data is structured uniformly enough across all three domains so the AI can generate contextual insights (e.g., "You have a family road trip to Munnar tomorrow, but your Tata Nexon insurance expires today, and your savings account needs funding to cover the trip budget.").