# Agent Registry

| | |
|---|---|
| **Type** | Reference |
| **Audience** | Developers, AI agents |
| **Status** | Active |
| **Last updated** | 2026-06-23 |

## Objective

Catalogue all Claude Code AI agents available in this repository, their responsibilities, and when to invoke each one. Agent definitions (full rules, bootstrap protocol, authority) live in `.claude/agents/` — this document is the index.

## Use Cases

- When starting a task and unsure which agent to use
- When reviewing which agent owns a particular file path or concern
- When adding a new agent — add it here after creating the definition in `.claude/agents/`

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

1. Create the definition file at `.claude/agents/<agent-name>.md`
2. Add a row to the Agent Roster table above
3. Keep agent scope narrow — one domain or one concern per agent, no overlapping authority
