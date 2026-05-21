# Context Primer — Suchika

| | |
|---|---|
| **Type** | Reference |
| **Audience** | AI agents, new developers |
| **Status** | Active |
| **Last updated** | 2026-06-24 |

## Objective

Provide a compact project snapshot that any agent or developer can read in ~2 minutes to get full context before starting work. This is the mandatory first read — everything else in `documents/` is depth on a specific topic.

## Use Cases

- **Always** — read this before starting any task in this repo
- After a long gap away from the project — re-sync on current version and domain status
- When handing off work to another agent or developer

---

**Read this first.** Compact project snapshot for fast agent bootstrapping. ~2 min read.
For depth, follow the links to domain-state files or documents/.

---

## What Is Suchika

Personal household management system. Four domains: Profile, Wealth, Health, Household.
Hexagonal Architecture (Ports & Adapters). Single PostgreSQL database (`app_db`). React frontend.

---

## Current Version: v0.3 — In Review (PR open)

| Domain | Backend | Frontend | Status |
|---|---|---|---|
| Profile | ✅ | ✅ | Complete |
| Wealth | ✅ | ✅ | Complete |
| Health | ✅ | ✅ | Complete |
| Household | ✅ | ✅ | Complete — v0.3 |

**Next milestone: v0.4** — Error handling (unhappy path, malformed CSV, quarantine protocol).

Quality gates (v0.3): 285 Jest tests passing, 0 SonarQube BLOCKER/CRITICAL issues, all Gradle tests green, ArchUnit clean.

**v0.3 key additions:**
- Household domain live: CalendarEvent, InventoryItem, Goal CRUD at port 8084
- ProjectionCalculationEngine in web-gateway: computes net worth, goal progress, vitals summary, event counts on demand
- Dashboard "Refresh Live Data" button wired to `POST /v1/projections/refresh/{profileId}`
- ADR-013 added: Projection Calculation Engine pattern

---

## Service Map

| Service | Port | DB Schema | Start command |
|---|---|---|---|
| profile | 8081 | `profile` | `./gradlew :application:domain:profile:adapters:quarkusDev` |
| wealth | 8082 | `wealth` | `./gradlew :application:domain:wealth:adapters:quarkusDev` |
| health | 8083 | `health` | `./gradlew :application:domain:health:adapters:quarkusDev` |
| household | 8084 | `household` | `./gradlew :application:domain:household:adapters:quarkusDev` |
| web-gateway | 8080 | `projections` | `./gradlew :application:web-gateway:quarkusDev` |
| frontend | 3000 | — | `cd web && npm start` |

**Startup order matters:** profile → wealth/health/household → gateway → frontend.
Frontend talks only to gateway (8080). Never call domain ports directly from React.

---

## Architecture in One Paragraph

Each domain is a Gradle sub-project with three layers: `domain/` (pure Java, zero framework deps), `ports/` (interfaces), `adapters/` (Quarkus/JPA/HTTP). ArchUnit enforces this. The `web-gateway` is a BFF that aggregates domain REST calls and has no DB of its own. All DB queries filtered by `profile_id` (injected in adapters only). No SQL ENUMs — VARCHAR with OpenAPI enum validation.

---

## Key Invariants (breaks build if violated)

1. `domain/` layer: zero `@Inject`, zero JPA, zero HTTP types — ArchUnit test enforces this
2. `profile_id` filter in every DB query — in adapter, never in domain
3. No SQL ENUMs — VARCHAR for all discriminators
4. Never edit a committed Flyway migration — create a new versioned file
5. Frontend never calls domain ports — only gateway at port 8080
6. `web/src/api/generated.ts` is never hand-edited — always `npm run generate:api`

---

## Domain State Files (read before working on a domain)

- [documents/domain-state/profile.md](domain-state/profile.md) — schema, files, open issues
- [documents/domain-state/wealth.md](domain-state/wealth.md) — schema, ADRs, backlog
- [documents/domain-state/health.md](domain-state/health.md) — schema, backlog
- [documents/domain-state/household.md](domain-state/household.md) — calendar events, inventory, goals; projection engine in gateway; v0.3 complete

---

## Agent Protocol

**Before starting any task:**
1. Read this file (CONTEXT_PRIMER.md)
2. Read the relevant `documents/domain-state/<domain>.md`
3. Read the specific source files for your task

**After completing any task:**
Update `documents/domain-state/<domain>.md` — mark done items, add new open issues, update schema if DB changed, update "last updated" date.

---

## Branch & PR Governance

Six governance files added to `.github/` in v0.2 post-release:

| File | Purpose |
|---|---|
| `.github/CODEOWNERS` | Defines required reviewers per path — code owners must approve before merge |
| `.github/pull_request_template.md` | Standard PR description template auto-loaded on PR creation |
| `.github/labeler.yml` | Path-to-label mapping used by the pr-labeler workflow |
| `.github/workflows/branch-name-check.yml` | Rejects branches that don't match `feat/`, `fix/`, `chore/`, `docs/`, `refactor/`, `test/` prefixes |
| `.github/workflows/pr-title-lint.yml` | Enforces Conventional Commits format on PR titles (e.g. `feat(wealth): add CSV upload`) |
| `.github/workflows/pr-labeler.yml` | Auto-labels PRs based on changed file paths (domain, frontend, infra, docs, etc.) |

CI workflow (`.github/workflows/ci.yml`) triggers on `main` branch only — dead `master` trigger removed.

---

## Where to Find Things

| Need | Location |
|---|---|
| Architecture rules | `documents/ARCHITECTURE_GUIDELINES.md` |
| Business requirements | `documents/BUSINESS_REQUIREMENTS.md` |
| Roadmap / milestones | `documents/ROADMAP.md` |
| Logging & exceptions | `documents/LOGGING_AND_EXCEPTIONS.md` |
| Frontend guidelines | `documents/FRONTEND_GUIDELINES.md` |
| E2E tests | `documents/E2E_TESTING.md` |
| Scripts / dev commands | `documents/SCRIPTS.md` |
| CI/CD pipeline | `documents/CICD.md` |
| PR governance | `.github/CODEOWNERS`, `.github/workflows/branch-name-check.yml`, `.github/workflows/pr-title-lint.yml` |
| OpenAPI contracts | `application/contract/<domain>.yaml` |
| Gateway contract | `application/contract/gateway.yaml` |
| Flyway migrations | `application/flyway/<domain>/` |
| Canonical code pattern | Copy from profile domain (it was the first) |

## Agent Roster

| Agent | Use for |
|---|---|
| `devops` | Scripts, startup, ports, DB, logs, lnav, CI — anything about running the system |
| `wealth-developer` | Accounts, transactions, CSV upload (port 8082) |
| `health-developer` | Vitals, doctor visits (port 8083) |
| `profile-developer` | Admin, member profiles (port 8081) |
| `household-developer` | Calendar, inventory, goals — v0.3 (port 8084) |
| `quarkus-developer` | Cross-domain backend Java work |
| `react-developer` | Cross-domain frontend work |
| `architect` | New domain design, ADRs, cross-domain patterns |
| `quality-manager` | SonarQube, ArchUnit, test coverage gates |
| `document-writer` | Update docs, SCRIPTS.md, domain-state files |
