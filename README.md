# Suchika (सूचिका)

A personal household management system covering **Profile**, **Wealth**, and **Health** domains with a React frontend. **v0.2 complete** — Profile identity, Wealth accounts/transactions/assets, and Health vital readings/doctor visits are all live.

Built on **Hexagonal Architecture** (Ports & Adapters) with four isolated Quarkus domain services behind a BFF web-gateway.

---

## Quick Start

1. **Clone & setup:** See [Contributing / Getting Started](./CONTRIBUTING.md)
2. **One-time DB bootstrap:**
```bash
psql -U postgres -c "CREATE DATABASE app_db;"
psql -U postgres -d app_db -f application/flyway/00_bootstrap.sql
```
3. **Start domain services (profile first — others FK into it):**
```bash
./gradlew :application:domain:profile:adapters:quarkusDev   # port 8081
./gradlew :application:domain:wealth:adapters:quarkusDev    # port 8082
./gradlew :application:domain:health:adapters:quarkusDev    # port 8083
./gradlew :application:web-gateway:quarkusDev               # port 8080 (BFF)
```
4. **Start frontend:**
```bash
cd web && npm install && npm start   # http://localhost:3000
```
5. **Open app:** `http://localhost:3000` — frontend talks only to the gateway at `http://localhost:8080`

---

## Documentation

| Document | Purpose |
|---|---|
| [CONTRIBUTING](./CONTRIBUTING.md) | Local dev setup, prerequisites, run commands |
| [ARCHITECTURE_GUIDELINES](./documents/ARCHITECTURE_GUIDELINES.md) | System design, hexagonal architecture, ADR index |
| [ARCHITECTURE_DECISIONS](./documents/ARCHITECTURE_DECISIONS.md) | All ADRs (ADR-001 through ADR-012) |
| [ARCHITECTURE_PROPOSALS](./documents/ARCHITECTURE_PROPOSALS.md) | Pending proposals under review |
| [BUSINESS_REQUIREMENTS](./documents/BUSINESS_REQUIREMENTS.md) | Functional specs, versioned epics, domain rules |
| [ROADMAP](./documents/ROADMAP.md) | Milestone plan v0.2 through v4.1 |
| [REQUIREMENTS_wealth_domain](./documents/REQUIREMENTS_wealth_domain.md) | Wealth domain feature specs |
| [REQUIREMENTS_health_domain](./documents/REQUIREMENTS_health_domain.md) | Health domain feature specs |
| [REQUIREMENTS_household_domain](./documents/REQUIREMENTS_household_domain.md) | Household domain feature specs (v0.3+) |
| [REQUIREMENTS_cross_domain](./documents/REQUIREMENTS_cross_domain.md) | Cross-domain and dashboard specs |
| [FRONTEND_GUIDELINES](./documents/FRONTEND_GUIDELINES.md) | React/Tailwind/ESLint standards |
| [E2E_TESTING](./documents/E2E_TESTING.md) | Playwright E2E test suite — setup, commands, conventions |
| [LOGGING_AND_EXCEPTIONS](./documents/LOGGING_AND_EXCEPTIONS.md) | AppLogger and exception hierarchy |
| [CICD](./documents/CICD.md) | Build and automation pipeline rules |
| [V02_DEVELOPMENT_PLAN](./documents/V02_DEVELOPMENT_PLAN.md) | v0.2 implementation plan and phase breakdown |
| [QA_API_TEST_RESULTS](./documents/QA_API_TEST_RESULTS.md) | API test results and QA sign-off |
| [AGENTS](./documents/AGENTS.md) | AI agent roles and responsibilities |
| [SECURITY](./SECURITY.md) | Vulnerability reporting and version support |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Backend Language | Java 25 |
| Backend Framework | Quarkus 3.29.0 |
| Build Tool | Gradle 9.3.0 (Kotlin DSL) |
| Database | PostgreSQL — single `app_db` with five schemas |
| Schema Migrations | Flyway (per-domain, auto-run on startup) |
| API Contract | OpenAPI 3.1.0 (contract-first) |
| API Style | Google AIP (resource-oriented) |
| Frontend | React (JavaScript) + Tailwind CSS |
| Architecture | Hexagonal (Ports & Adapters) |
| Code Quality | SonarQube Community Edition (local analysis) |

---

## Repository Structure

```
suchika/
├── .claude/                             <- Claude Code agent definitions
│   └── agents/
├── .github/                             <- GitHub workflows, issue templates, Copilot agents
│   ├── copilot/agents/
│   ├── ISSUE_TEMPLATE/
│   └── workflows/
├── application/
│   ├── contract/                        <- OpenAPI contracts (source of truth)
│   │   ├── gateway.yaml                 <- BFF contract (frontend generates typed client from this)
│   │   ├── health.yaml
│   │   ├── household.yaml
│   │   ├── profile.yaml
│   │   └── wealth.yaml
│   ├── domain/
│   │   ├── health/                      <- Health domain (port 8083)
│   │   │   ├── adapters/                <- JAX-RS controllers + Panache persistence
│   │   │   ├── domain/                  <- Pure Java business logic (no framework deps)
│   │   │   └── ports/                   <- Input/output port interfaces
│   │   ├── household/                   <- Household domain (port 8084, v0.3)
│   │   │   ├── adapters/
│   │   │   ├── domain/
│   │   │   └── ports/
│   │   ├── profile/                     <- Profile domain (port 8081) — start this first
│   │   │   ├── adapters/
│   │   │   ├── domain/
│   │   │   └── ports/
│   │   └── wealth/                      <- Wealth domain (port 8082)
│   │       ├── adapters/
│   │       ├── domain/
│   │       └── ports/
│   ├── flyway/                          <- Flyway migrations (per domain, auto-run on startup)
│   │   ├── 00_bootstrap.sql             <- Run manually once as superuser before first start
│   │   ├── health/
│   │   ├── household/
│   │   ├── profile/
│   │   ├── projections/
│   │   ├── test-seed/
│   │   └── wealth/
│   └── web-gateway/                     <- BFF aggregator (port 8080) — no DB, composes domain calls
├── assets/
│   └── images/
├── documents/                           <- All project documentation
│   ├── AGENTS.md
│   ├── ARCHITECTURE_DECISIONS.md        <- ADR-001 through ADR-012
│   ├── ARCHITECTURE_GUIDELINES.md
│   ├── ARCHITECTURE_PROPOSALS.md
│   ├── BUSINESS_REQUIREMENTS.md
│   ├── CICD.md
│   ├── E2E_TESTING.md                   <- Playwright E2E test suite docs
│   ├── FRONTEND_GUIDELINES.md
│   ├── LOGGING_AND_EXCEPTIONS.md
│   ├── QA_API_TEST_RESULTS.md
│   ├── REQUIREMENTS_cross_domain.md
│   ├── REQUIREMENTS_health_domain.md
│   ├── REQUIREMENTS_household_domain.md
│   ├── REQUIREMENTS_wealth_domain.md
│   ├── ROADMAP.md
│   └── V02_DEVELOPMENT_PLAN.md
├── gradle/wrapper/
├── infrastructure/local/                <- Local env config (.env.template)
├── scripts/                             <- Dev helper scripts (PowerShell + bash)
├── shared/                              <- Cross-domain: AppLogger, exceptions, ArchUnit tests
│   └── src/
│       ├── main/java/com/suchika/shared/
│       │   ├── dto/
│       │   ├── exception/
│       │   ├── logging/
│       │   └── mapper/
│       └── test/java/com/suchika/architecture/   <- ArchUnit domain rules
└── web/                                 <- React frontend (talks only to gateway)
    ├── e2e/                             <- Playwright E2E specs (17 tests)
    │   ├── auth.spec.js
    │   ├── health.spec.js
    │   ├── navigation.spec.js
    │   ├── profiles.spec.js
    │   └── wealth.spec.js
    ├── playwright.config.js             <- Chromium, headless, baseURL :3000
    ├── public/
    └── src/
        ├── api/generated.ts             <- Auto-generated from gateway.yaml (do not hand-edit)
        ├── components/
        ├── context/
        ├── hooks/
        ├── pages/
        │   ├── Admin/
        │   ├── Health/
        │   ├── Household/
        │   ├── Public/
        │   ├── User/
        │   └── Wealth/
        ├── types/
        └── utils/
```

---

## Architecture Overview

Four isolated Quarkus domain services, each following Hexagonal Architecture (Ports & Adapters):

```
application/domain/
├── profile/    ← identity anchor; every other domain FKs into profile.profile (port 8081)
├── wealth/     ← accounts, transactions, physical assets (port 8082)
├── health/     ← vital readings, doctor visits (port 8083)
└── household/  ← calendar, grocery inventory, goals (deferred to v0.3, port 8084)

application/web-gateway/   ← BFF aggregator; frontend talks only here (port 8080)
```

Each domain has three layers:
- `domain/` — pure Java business logic, zero framework dependencies
- `ports/` — input ports (use cases) and output ports (persistence contracts)
- `adapters/` — JAX-RS controllers and Panache/JPA persistence

**Key rule:** `domain/` must have zero framework dependencies. Enforced by ArchUnit in `shared/`.

---

## Database

Single PostgreSQL database (`app_db`) with five schemas:

| Schema | Content |
|---|---|
| `profile` | `admin`, `profile` — household manager and all members |
| `wealth` | `account`, `transaction`, `statement_upload`, `physical_asset` |
| `health` | `vital_reading`, `doctor_visit` |
| `household` | `calendar_event`, `inventory_item`, `goal` (v0.3) |
| `projections` | `dashboard_snapshot` — CQRS read model owned by web-gateway |

Each domain owns its schema. No cross-domain SQL joins.

---

## Current Status: v0.2 Complete

**Shipped in v0.2:**
- Profile domain — household members and admin identity
- Wealth domain — accounts, CSV transaction upload, physical assets
- Health domain — vital readings (weight, BP, blood sugar), doctor visits
- Web-gateway BFF — dashboard snapshot projection (CQRS)
- React frontend — wealth, health, and profile pages

**Next: v0.3 — Household Domain**
- Calendar events, grocery inventory, household goals

See [ROADMAP](./documents/ROADMAP.md) and [BUSINESS_REQUIREMENTS](./documents/BUSINESS_REQUIREMENTS.md) for the full milestone plan.

---

## Testing

### Unit tests (Jest — 79 tests)

```bash
cd web && npm run test:ci   # single run, no watch (matches CI)
```

### Backend tests

```bash
./gradlew test              # all modules
./gradlew :application:domain:wealth:domain:test   # single module example
```

### E2E tests (Playwright — 17 tests)

Playwright tests cover auth, navigation, profiles, wealth, and health flows.

**Startup order:**

```bash
./gradlew :application:domain:profile:adapters:quarkusDev   # port 8081 — start FIRST
./gradlew :application:domain:wealth:adapters:quarkusDev    # port 8082
./gradlew :application:domain:health:adapters:quarkusDev    # port 8083
./gradlew :application:web-gateway:quarkusDev               # port 8080 (BFF)
cd web && npm start                                          # port 3000
```

**Run tests** (in a second terminal, after dev server is up):

```bash
cd web && npm run test:e2e          # headless
cd web && npm run test:e2e:headed   # visible browser
cd web && npm run test:e2e:report   # open HTML report
```

Page-load and navigation tests pass without a backend running — only the dev server is required. See [E2E_TESTING](./documents/E2E_TESTING.md) for full details.

---

## Contributing

1. Read [ARCHITECTURE_GUIDELINES](./documents/ARCHITECTURE_GUIDELINES.md) to understand the design
2. Follow Hexagonal Architecture rules — `domain/` is framework-free
3. Start profile service first — other domains FK into `profile.profile`
4. Keep Flyway migrations sequential; never edit a committed migration
5. Run `./gradlew test` before committing

See [CONTRIBUTING](./CONTRIBUTING.md) for full setup instructions.

---

## Support

- **Setup issues?** → [CONTRIBUTING](./CONTRIBUTING.md)
- **Architecture rules?** → [ARCHITECTURE_GUIDELINES](./documents/ARCHITECTURE_GUIDELINES.md)
- **Business rules?** → [BUSINESS_REQUIREMENTS](./documents/BUSINESS_REQUIREMENTS.md)
- **Security issues?** → [SECURITY](./SECURITY.md)