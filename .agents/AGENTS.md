# Agent Registry

| | |
|---|---|
| **Type** | Reference |
| **Audience** | Developers, AI agents |
| **Status** | Active |
| **Last updated** | 2026-06-23 |

## Objective

Catalogue all native AI Agent Skills available in this repository, their responsibilities, and when to invoke each one. Agent definitions (full rules, bootstrap protocol, authority) live in `.agents/skills/` — this document is the index.
The Architect agent (you) can act as the orchestrator and dispatch tasks to these specialized skills.

## Use Cases

- When starting a task and unsure which agent to use
- When reviewing which agent owns a particular file path or concern
- When adding a new agent — add it here after creating the definition in `.agents/skills/<agent-name>/SKILL.md`

---

## Agent Roster

| Agent | Role | Use When |
|---|---|---|
| `architect` | Architecture designer | Designing new domains, proposing ADRs, reviewing hexagonal compliance, planning structural changes |
| `business-analyst` | Business analyst | Writing acceptance criteria, scoping features to milestones, evaluating domain boundary violations, updating requirements documents |
| `devops` | DevOps / infrastructure | Scripts, service startup, port conflicts, database operations, logs, CI/CD pipeline, SonarQube |
| `document-writer` | Documentation consolidator | Consolidating markdown into `/documents/`, updating README tree, fixing broken doc links after file moves |
| `health-developer` | Health domain specialist | All backend and frontend work in the health domain — vital readings, doctor visits (port 8083) |
| `household-developer` | Household domain specialist | All backend and frontend work in the household domain — calendar, inventory, goals (port 8084, v0.3) |
| `profile-developer` | Profile domain specialist | All backend and frontend work in the profile domain — admin, household members (port 8081) |
| `quality-manager` | Quality manager | Test coverage reviews, build stability, ArchUnit audits, SonarQube analysis, pre-commit hooks |
| `quarkus-developer` | Backend Quarkus developer | Cross-domain Java work — domain code, Panache repos, JAX-RS controllers, Flyway migrations, OpenAPI contracts |
| `react-developer` | Frontend React developer | Cross-domain frontend work — components, hooks, pages, Tailwind, routing, generated API client |
| `wealth-developer` | Wealth domain specialist | All backend and frontend work in the wealth domain — accounts, transactions, CSV uploads, physical assets (port 8082) |
| `Explore` | Read-only codebase search | Locating files by pattern, finding symbol definitions, answering "where is X?" without modifying anything |
| `Plan` | Implementation planner | Designing implementation strategy, identifying critical files, evaluating architectural trade-offs before writing code |

---

## Domain Agent Priority

When a task is scoped to a single domain, prefer the domain-specific agent over the general ones:

```
task in health/    → health-developer    (not quarkus-developer or react-developer)
task in wealth/    → wealth-developer
task in profile/   → profile-developer
task in household/ → household-developer
cross-domain task  → quarkus-developer (backend) or react-developer (frontend)
```

---

## Adding a New Agent

1. Create the definition file at `.agents/skills/<agent-name>/SKILL.md`
2. Add a row to the Agent Roster table above
3. Keep agent scope narrow — one domain or one concern per agent, no overlapping authority

---

## Agent Protocol & Bootstrap (Inherited from CLAUDE.md & CONTEXT_PRIMER.md)

**Before starting any task:**
1. Read `documents/CONTEXT_PRIMER.md` (compact project snapshot).
2. Read the relevant `documents/domain-state/<domain>.md` (schema, files, open issues).
3. Read the specific source files for your task.

**After completing any task:**
Update `documents/domain-state/<domain>.md` — mark done items, add new open issues, update schema if DB changed, update "last updated" date.

---

## Development Guidelines

### Commands & Dev Aliases
Dot-source once per terminal: `. .\scripts\dev-aliases.ps1` (PowerShell) or `. ./scripts/dev-aliases.sh` (bash/Codespaces).
- `dp` / `dw` / `dh` / `dho` / `dg` / `dwb`: Start profile/wealth/health/household/gateway/web in dev mode (visible window). Start `dp` first.
- `da`: Start everything in dependency order (visible windows) for active development.
- `rl` (run-local): Start everything headlessly. Use `stopl` to stop.
- `tp` / `tw` / `tsa`: Test one domain / all backend tests.
- Frontend: `npm install`, `npm run generate:api`, `npm start`, `npm run test:ci` in `web/`

### Architecture & Rules
Suchika is a personal household management system using **Hexagonal Architecture (Ports & Adapters)** with four domains: Profile (8081), Wealth (8082), Health (8083), Household (8084).
- `web-gateway` (8080) is a BFF aggregator for cross-domain dashboard data. It composes domain REST calls via MicroProfile Rest Client and runs CQRS projections.
- **Frontend talks only to the gateway at `http://localhost:8080`**.
- **Domain Layer (`domain/`)**: Must have ZERO framework dependencies. No `@Inject`, no JPA, no HTTP. Enforced by ArchUnit.
- **Database**: Single PostgreSQL DB (`app_db`) with schemas for each domain (`profile`, `wealth`, `household`, `health`, `projections`).
- **Profile-scoped isolation**: Every DB query across all domains must filter by the active `profile_id` (injected in the `adapters/` layer).
- **Flyway Migrations**: Never edit a committed migration — create a new versioned file.
- **Database Constraints**: Keep NOT NULL, PK, FK, UNIQUE in DB. **No CHECK constraints of any kind** (including enum discriminators or business rules). Business rules go to the domain layer (static factory methods throwing `IllegalArgumentException`).
- **No SQL ENUMs ever**: Use plain `VARCHAR` (capped at 50 for names).
- **Timezones**: Always IST (UTC+5:30).
- **Frontend structure**: Custom hooks or `useEffect` for API calls, never in component render. Tailwind CSS only. No CSS modules.
- **Logging & Exceptions**: Use `AppLogger` (INFO, WARNING, ERROR, HEALTH) from `shared/`. **No DEBUG level anywhere**. Throw typed exceptions from `shared/exception/`.

### CQRS Dashboard Projections
Calculations run once and are stored in `projections.dashboard_snapshot` via UPSERT. The calculation engine lives in `web-gateway`.

### Useful Documentation References
- [ARCHITECTURE_GUIDELINES.md](documents/ARCHITECTURE_GUIDELINES.md) — architectural rules.
- [BUSINESS_REQUIREMENTS.md](documents/BUSINESS_REQUIREMENTS.md) — feature specs.
- [FRONTEND_GUIDELINES.md](documents/FRONTEND_GUIDELINES.md) — React/Tailwind/ESLint standards.
- [LOGGING_AND_EXCEPTIONS.md](documents/LOGGING_AND_EXCEPTIONS.md) — shared logger and exception usage.
- [SCRIPTS.md](documents/SCRIPTS.md) — scripts reference.
