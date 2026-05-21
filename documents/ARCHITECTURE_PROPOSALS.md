# Architecture Proposals

> Open questions. Each needs a team decision before implementation.
> Once decided, move the outcome to ARCHITECTURE_DECISIONS.md and close the proposal.

---

## PROP-001: Microservices Split

**Status:** Resolved — see ADR-002

**Decision made:** Four domain services + one BFF (web-gateway), each on its own port. Profile (8081), Wealth (8082), Health (8083), Household (8084), Web Gateway (8080). All share one PostgreSQL database with schema-per-domain isolation.

---

## PROP-002: Cross-Domain Data in Restricted Profiles

**Status:** Open

**Context:** The current rule is that Child profiles cannot query Wealth. But a future trip feasibility endpoint will check trip budget (Wealth) and vehicle compliance. Should this endpoint be accessible to Child profiles with redacted data, or blocked entirely?

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

**Context:** Current endpoints are under `/v1/`. No formal versioning strategy exists yet.

**Option A — URL versioning (current approach, extend it)**
- Keep `/v1/` and add `/v2/` when breaking changes are needed.
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

**Context:** The frontend is a React app using Context API for auth state. As cross-domain views (Unified Dashboard, Vacation Planner) grow, local component state will not be enough.

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
