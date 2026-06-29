# Suchika (सूचिका)

A personal household management system — track your **Wealth**, **Health**, and **Household** data in one place, owned and run locally.

Built on **Hexagonal Architecture** (Ports & Adapters) with four isolated Quarkus microservices, a BFF web-gateway, and a React frontend.

**Current version: v0.4** — All four domains live. Wealth CSV upload has structured error logging, dedup fix, and frontend error/skipped-duplicates panels.

---

## What It Does

| Domain | What You Can Track |
|---|---|
| **Profile** | Household members and admin identity — the identity anchor for all other domains |
| **Wealth** | Bank accounts, transactions, CSV bank statement uploads, physical assets |
| **Health** | Vital readings (weight, blood pressure, blood sugar), doctor visits |
| **Household** | Calendar events, grocery inventory, household goals |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21 · Quarkus 3.29.0 · Gradle 9.3.0 |
| Database | PostgreSQL — single `app_db`, schema-per-domain |
| Migrations | Flyway (per-domain, auto-run on startup) |
| API | OpenAPI 3.1.0 contract-first · Google AIP style |
| Frontend | React (JavaScript) · Tailwind CSS |
| Architecture | Hexagonal (Ports & Adapters) · ArchUnit-enforced |
| Code Quality | SonarQube Community Edition |

---

## Quick Start

Full setup instructions are in [CONTRIBUTING.md](./CONTRIBUTING.md). The short version:

```bash
# 1. One-time database bootstrap (run as superuser)
psql -U postgres -c "CREATE DATABASE app_db;"
psql -U postgres -d app_db -f application/flyway/00_bootstrap.sql

# 2. Start backend services — profile MUST start first (other domains FK into it)
./gradlew :application:domain:profile:adapters:quarkusDev   # port 8081
./gradlew :application:domain:wealth:adapters:quarkusDev    # port 8082
./gradlew :application:domain:health:adapters:quarkusDev    # port 8083
./gradlew :application:web-gateway:quarkusDev               # port 8080 (BFF)

# 3. Start the frontend
cd web && npm install && npm start   # http://localhost:3000
```

> The frontend talks **only** to the gateway at port 8080 — never to domain services directly.

---

## Where to Go for More

Organised by what you need to do:

### Getting started / running the app
- [CONTRIBUTING.md](./CONTRIBUTING.md) — prerequisites, one-time setup, dev aliases, troubleshooting

### Understanding the architecture
- [ARCHITECTURE_GUIDELINES](./documents/ARCHITECTURE_GUIDELINES.md) — rules every PR must follow (hexagonal layers, domain isolation, DB constraints)
- [ARCHITECTURE_DECISIONS](./documents/ARCHITECTURE_DECISIONS.md) — all ADRs (ADR-001 through ADR-012) with rationale
- [ARCHITECTURE_PROPOSALS](./documents/ARCHITECTURE_PROPOSALS.md) — pending proposals under review

### Understanding what's built and what's planned
- [BUSINESS_REQUIREMENTS](./documents/BUSINESS_REQUIREMENTS.md) — functional specs, domain rules, milestone roadmap (v0.1 → v4.1)
- [ROADMAP](./documents/ROADMAP.md) — milestone breakdown with shipped vs. planned features
- Domain-specific requirements:
  [Wealth](./documents/REQUIREMENTS_wealth_domain.md) ·
  [Health](./documents/REQUIREMENTS_health_domain.md) ·
  [Household](./documents/REQUIREMENTS_household_domain.md) ·
  [Cross-domain](./documents/REQUIREMENTS_cross_domain.md)

### Writing code
- [ARCHITECTURE_GUIDELINES](./documents/ARCHITECTURE_GUIDELINES.md) — non-negotiable rules; ArchUnit enforces them in CI
- [FRONTEND_GUIDELINES](./documents/FRONTEND_GUIDELINES.md) — React, Tailwind, ESLint, routing, auth conventions
- [LOGGING_AND_EXCEPTIONS](./documents/LOGGING_AND_EXCEPTIONS.md) — `AppLogger` and the typed exception hierarchy
- [domain-state files](./documents/domain-state/) — current schema, open issues, and ADRs per domain

### CI, branching, and pull requests
- [CICD](./documents/CICD.md) — pipeline jobs, branch naming rules, PR title format, governance workflows
- [CONTRIBUTING.md](./CONTRIBUTING.md) — branch/PR naming quick-reference with examples

### Running tests
- [CONTRIBUTING.md](./CONTRIBUTING.md) — backend and frontend test commands
- [E2E_TESTING](./documents/E2E_TESTING.md) — Playwright E2E suite: setup, startup order, writing new tests

### Scripts and dev tooling
- [SCRIPTS](./documents/SCRIPTS.md) — every script in `scripts/` with parameters and dev-alias shortcuts

### AI agents working in this repo
- [CONTEXT_PRIMER](./documents/CONTEXT_PRIMER.md) — compact project snapshot; read this first before starting any task
- [AGENTS](./documents/AGENTS.md) — full agent roster, when to use each agent, and how to add new ones

---

## 📁 Repository Structure

```
suchika/
├── application/
│   ├── contract/                  ← OpenAPI contracts (source of truth for all API types)
│   │   ├── gateway.yaml           ← BFF contract — frontend generates typed client from this
│   │   ├── profile.yaml
│   │   ├── wealth.yaml
│   │   ├── health.yaml
│   │   └── household.yaml
│   ├── domain/
│   │   ├── profile/               ← port 8081 — start first; all other domains FK into profile.profile
│   │   │   ├── domain/            ← pure Java business logic, zero framework deps
│   │   │   ├── ports/             ← input/output port interfaces
│   │   │   └── adapters/          ← Quarkus/JPA/HTTP implementations
│   │   ├── wealth/                ← port 8082 — accounts, transactions, CSV uploads, physical assets
│   │   │   ├── domain/
│   │   │   ├── ports/
│   │   │   └── adapters/
│   │   ├── health/                ← port 8083 — vital readings, doctor visits
│   │   │   ├── domain/
│   │   │   ├── ports/
│   │   │   └── adapters/
│   │   └── household/             ← port 8084 — calendar events, inventory, goals
│   │       ├── domain/
│   │       ├── ports/
│   │       └── adapters/
│   ├── flyway/                    ← Flyway migrations per domain; 00_bootstrap.sql run manually once
│   │   ├── 00_bootstrap.sql       ← manual one-time superuser setup
│   │   ├── profile/
│   │   ├── wealth/
│   │   ├── health/
│   │   ├── household/
│   │   └── gateway/
│   └── web-gateway/               ← port 8080; BFF aggregator — no DB, composes domain REST calls
├── web/                           ← React + Tailwind frontend; talks only to web-gateway (port 8080)
│   └── src/
│       ├── api/                   ← generated.ts (never hand-edit) + domain API modules
│       └── pages/
│           ├── Public/
│           ├── User/              ← wealth/, health/, household/ pages
│           └── Admin/
├── shared/                        ← AppLogger, typed exceptions, ArchUnit domain rules
├── scripts/                       ← PowerShell + bash dev helpers (see documents/SCRIPTS.md)
├── documents/                     ← All project documentation
│   ├── CONTEXT_PRIMER.md          ← read this first — compact project snapshot
│   ├── domain-state/              ← per-domain schema, ADRs, open issues
│   │   ├── profile.md
│   │   ├── wealth.md
│   │   ├── health.md
│   │   └── household.md
│   ├── ARCHITECTURE_GUIDELINES.md
│   ├── ARCHITECTURE_DECISIONS.md
│   ├── BUSINESS_REQUIREMENTS.md
│   ├── ROADMAP.md
│   ├── FRONTEND_GUIDELINES.md
│   ├── LOGGING_AND_EXCEPTIONS.md
│   ├── SCRIPTS.md
│   ├── CICD.md
│   └── E2E_TESTING.md
├── .github/
│   ├── CODEOWNERS                 ← * @ketan
│   ├── workflows/
│   │   ├── ci.yml
│   │   ├── branch-name-check.yml  ← pattern: ^[a-zA-Z][a-zA-Z0-9_-]{3,}$
│   │   ├── pr-title-lint.yml
│   │   └── pr-labeler.yml
│   ├── labeler.yml
│   └── pull_request_template.md
├── .devcontainer/                 ← Codespaces: app container (Java 17 + Node 18) + db container (PG 16)
├── README.md
├── CONTRIBUTING.md
├── CLAUDE.md
├── CODE_OF_CONDUCT.md
└── SECURITY.md
```

Each domain follows three layers: `domain/` (pure Java, zero framework deps) → `ports/` (interfaces) → `adapters/` (Quarkus/JPA/HTTP).

---

## Contributing

1. Branch names must match `^[a-zA-Z][a-zA-Z0-9_-]{3,}$` — starts with a letter, min 4 chars, letters/digits/hyphens/underscores only *(CI rejects non-conforming branches; no type-prefix required)*
2. PR titles must follow Conventional Commits — e.g. `feat(wealth): add CSV upload` *(enforced by `pr-title-lint` workflow)*
3. `domain/` layer must have zero framework imports — ArchUnit fails the build if violated
4. Never edit a committed Flyway migration — create a new versioned file

Full setup and contribution guide: [CONTRIBUTING.md](./CONTRIBUTING.md)

---

## Support

| Question | Where to look |
|---|---|
| Setup or startup issues | [CONTRIBUTING.md](./CONTRIBUTING.md) |
| Architecture rules | [ARCHITECTURE_GUIDELINES](./documents/ARCHITECTURE_GUIDELINES.md) |
| Business / domain rules | [BUSINESS_REQUIREMENTS](./documents/BUSINESS_REQUIREMENTS.md) |
| Security vulnerabilities | [SECURITY.md](./SECURITY.md) |
