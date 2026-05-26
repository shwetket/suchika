# Architecture Proposals

> Open questions. Each needs a team decision before implementation.
> Once decided, move the outcome to ARCHITECTURE_DECISIONS.md and close the proposal.

---

## PROP-001: Microservices Split

**Status:** Open

**Context:** Currently all three domains run in a single Quarkus runtime. As the system grows, independent deployment and scaling may be needed.

**Option A — Stay monolith (status quo)**
- Keep all modules in one Quarkus runtime.
- Pro: simpler ops, no network overhead between domains.
- Con: one domain's load spike affects all; single deploy unit.

**Option B — Split into 3 Quarkus services**
- `wealth-service`, `health-service`, `household-service` each deploy independently.
- Pro: independent scaling and deployment.
- Con: introduces network latency for cross-domain calls, more infra to manage.

**Option C — Extract only Health service (hybrid)**
- Health is already on MongoDB and is read-heavy. Extract it first as a pilot.
- Pro: lower risk than full split; tests the approach.
- Con: inconsistent architecture during transition.

**Decision needed:** Which option? Who decides? Target date?

---

## PROP-002: Cross-Domain Data in Restricted Profiles

**Status:** Open

**Context:** The current rule is that Child profiles cannot query Wealth. But the `/api/v1/trips/{event_id}/feasibility` endpoint checks trip budget (Wealth) and vehicle compliance. Should this endpoint be accessible to Child profiles with redacted data, or blocked entirely?

**Option A — Block entirely for restricted profiles**
- Simple. Consistent with current RBAC rules.
- Con: Child can't see trip feasibility at all, even for basic "is this trip happening" info.

**Option B — Allow with redacted response**
- Feasibility endpoint returns a simplified response for Child profiles (go/no-go only, no financial details).
- Pro: better UX for families.
- Con: more complex endpoint logic; needs careful audit to avoid data leaks.

**Decision needed:** Which option?

---

## PROP-003: Event Sourcing for Wealth Transactions

**Status:** Open

**Context:** Transaction ledgers are append-only by nature. An event sourcing model could improve auditability and replay capability.

**Option A — Keep current CRUD model**
- Simpler. Current Flyway migrations already handle schema.
- Con: harder to reconstruct state at a point in time.

**Option B — Adopt event sourcing for `wealth` domain only**
- Transactions become immutable events. Current state is a projection.
- Pro: full audit trail, time-travel queries.
- Con: significant complexity increase; team needs to learn the pattern.

**Decision needed:** Is auditability worth the complexity? Any timeline pressure?

---

## PROP-004: API Versioning Strategy

**Status:** Open

**Context:** Current endpoints are under `/api/v1/`. Cross-domain endpoints are marked `v0.5+`. No formal versioning strategy exists yet.

**Option A — URL versioning (current approach, extend it)**
- Keep `/api/v1/` and add `/api/v2/` when breaking changes are needed.
- Pro: simple, widely understood.
- Con: can lead to long-lived parallel versions.

**Option B — Header-based versioning**
- Single URL, version negotiated via `Accept` or custom header.
- Pro: cleaner URLs.
- Con: harder to test in browser, less visible.

**Option C — Deprecation-first policy with no parallel versions**
- Only one active version at a time. Breaking changes require migration, not a new version.
- Pro: no versioning debt.
- Con: forces all clients to upgrade together.

**Decision needed:** Which strategy before v1.0 launch?

---

## PROP-005: Frontend State Management

**Status:** Open

**Context:** The frontend is a React app. No state management library is specified in the current architecture doc. As cross-domain views (Unified Dashboard, Vacation Planner) grow, local component state will not be enough.

**Option A — React Query + local state only**
- Use React Query for server state, `useState`/`useReducer` for local UI state.
- Pro: lightweight, no extra library.
- Con: harder to share state across domain views.

**Option B — Redux Toolkit**
- Global store with domain slices.
- Pro: predictable, good devtools.
- Con: boilerplate; may be overkill for current scope.

**Option C — Zustand**
- Lightweight global store.
- Pro: minimal boilerplate, easy to colocate with features.
- Con: less opinionated — needs team conventions.

**Decision needed:** Which approach before building the Unified Dashboard?
