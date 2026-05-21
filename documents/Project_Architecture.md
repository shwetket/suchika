# Project Architecture

This document describes the system design, hexagonal architecture, and key structural decisions.

---

## Architecture — Hexagonal (Ports and Adapters)

Each domain (`Finance`, `Health`) is a self-contained hexagon. The core rule is that **domain never depends on anything outside itself**.

```
                    ┌──────────────────────────────────┐
                    │           DOMAIN                 │
    HTTP Request ──► │  ports/in  →  application  →     │ ──► Database
                          │  (use case interfaces)           │ ──► External API
                    │            ↕                     │
                    │         domain/                  │
                    │  (entities, services, logic)     │
                    │            ↕                     │
                    │  ports/out                       │
                    │  (repository interfaces)         │
                    └──────────────────────────────────┘
                           ↑                  ↑
                    adapters/in/        adapters/out/
                    (controllers)       (DB, HTTP clients)
```

### Key Rules

| Layer | Rules |
|---|---|
| **domain/** | Zero imports from adapters, infrastructure, or framework. Plain Java only. |
| **ports/in/** | Interfaces defining use cases. Implemented by `application/` classes. |
| **ports/out/** | Interfaces defining what domain needs from outside. Implemented by `adapters/out/`. |
| **application/** | Use case implementations only. Orchestrates domain logic. No direct DB or HTTP calls. |
| **adapters/out/** | Imports from `infrastructure/` for shared clients. Never imports from another domain. |
| **infrastructure/** | Shared wiring only (DB pools, config). No business logic. |
| **shared/** | Cross-cutting utilities (auth, logging, errors). No domain dependencies. |

**Cross-domain rule:** Domains never import each other. The unified app preserves isolation through package boundaries and clear ports/adapters separation.

---

## Repository File Structure

```
suchika/
├── application/                  # Backend domain service
│   ├── finance/                  # Unified Quarkus application
│   │   ├── build.gradle.kts      # Quarkus dependencies and application config
│   │   └── src/main/
│   │       ├── java/com/suchika/finance/
│   │       │   ├── domain/
│   │       │   ├── ports/in/
│   │       │   ├── ports/out/
│   │       │   ├── application/
│   │       │   ├── adapters/in/http/
│   │       │   ├── adapters/out/persistence/
│   │       │   ├── csv/
│   │       │   └── mappers/
│   │       ├── java/com/suchika/health/
│   │       │   ├── domain/
│   │       │   ├── ports/in/
│   │       │   ├── ports/out/
│   │       │   ├── application/
│   │       │   ├── adapters/in/http/
│   │       │   ├── adapters/out/persistence/
│   │       │   └── config/
│   │       └── resources/
│   │           ├── application.properties           # Config and shared Quarkus settings
│   │           └── db/migration/
│   │               ├── V1__create_account_table.sql
│   │               ├── V2__create_transaction_table.sql
│   │               ├── V3__create_goal_table.sql
│   │               ├── V4__create_health_profile_table.sql
│   │               ├── V5__create_doctor_visit_table.sql
│   │               └── V6__create_vital_reading_table.sql
│
├── infrastructure/               # Shared plumbing
│   ├── build.gradle.kts
│   └── src/main/java/com/suchika/infrastructure/
│       ├── persistence/         # Shared DB connection pool
│       └── config/              # Shared configuration
│
├── shared/                       # Cross-cutting concerns (auth, logging, errors)
│   ├── build.gradle.kts
│   └── src/main/java/com/suchika/shared/
│       ├── auth/                # (Stub for Phase 3)
│       ├── logging/
│       ├── errors/              # Error types, exception handling
│       └── utils/
│
├── openapi/                      # OpenAPI contracts (contract-first)
│   ├── finance.yaml              # Finance API specification
│   └── health.yaml               # Health API specification
│
├── ui/                           # Frontend monorepo
│   └── web/                      # React (JavaScript) app
│       ├── package.json
│       ├── src/
│       │   ├── App.js            # Main component
│       │   ├── App.css
│       │   ├── index.js          # Entry point
│       │   └── api/generated/    # Auto-generated API clients from OpenAPI
│       │       ├── finance.client.js
│       │       └── health.client.js
│       └── public/
│           ├── index.html        # HTML template
│           └── manifest.json     # PWA manifest
│
├── build.gradle.kts              # Root Gradle build (aggregator)
├── settings.gradle.kts           # Module registration
├── gradle/wrapper/               # Gradle wrapper (9.3.0)
├── GETTING_STARTED.md            # Local dev setup guide
├── README.md                     # Project overview
├── documents/
│   ├── Business_Requirement.md   # Full specs, requirements, data model
│   ├── Project_Architecture.md   # This file
│   ├── User_Guide.md             # End-user documentation
│   └── ROADMAP.md                # Phases 2–4 planning
│
└── gradle.properties             # JVM and Gradle configuration
```

---

## Data Model

### Single Database (`app_db`)

All domains share one PostgreSQL database with clearly separated tables. Each domain owns its own tables — no cross-domain joins.

### Finance Domain (Phase 1)

| Table | Purpose |
|---|---|
| `banking_transaction` | Stores bank/credit card/loan transactions. Composite UNIQUE constraint: `(account_name, account_type, date, amount, txn_type)` for deduplication. |
| `investment_transaction` | Stores fund transactions. Composite UNIQUE constraint: `(fund_name, date, amount, txn_type)` for deduplication. |

**Key design decisions:**
- `amount` always positive; direction indicated by `txn_type` (CREDIT/DEBIT)
- `is_duplicate` flag instead of rejection — overlapping uploads are still inserted for Phase 2 review UI
- `balance` column optional — populated only when CSV contains it
- `metadata` (JSONB) for investment extras (units, NAV, etc.)

### Health Domain

| Table | Purpose |
|---|---|
| `health_profile` | One row per person (self + family members) |
| `doctor_visit` | Doctor visits and illness periods |
| `vital_reading` | BP, weight, blood sugar, heart rate — time series |

*(Schema defined in Flyway migrations V3–V5. Details in [Business_Requirement.md](./Business_Requirement.md).)*

---

## API Contract — OpenAPI 3.1.0

Contracts written first in `openapi/` before backend code. Backend controllers must match contract. Frontend auto-generates clients.

### API Conventions

| Convention | Rule |
|---|---|
| **Base path** | `/api/v1` |
| **Resource URLs** | Plural nouns — `/api/v1/transactions`, `/api/v1/health-profiles` |
| **Standard methods** | `GET`, `POST`, `PATCH`, `DELETE` per Google AIP |
| **Custom methods** | `POST /api/v1/transactions:uploadCsv` (`:verb` pattern for non-CRUD actions) |
| **Field names** | `snake_case` throughout |
| **Error format** | `{ code, status, message, details }` |

**Example endpoints:**
```
POST   /api/v1/transactions:uploadCsv   # Upload CSV (custom method)
GET    /api/v1/transactions             # List with pagination
GET    /api/v1/transactions:config      # Get ENUM dropdowns
```

---

## Build System — Gradle (Kotlin DSL)

### Root `build.gradle.kts`

Aggregate build for all modules:
```gradle
allprojects {
    group = "com.suchika"
    version = "1.0.0"
}

subprojects {
    apply(plugin = "java")
    // ... common config
}
```

### Module-specific `build.gradle.kts`

- `application/finance/build.gradle.kts` — depends on `:shared`, `:infrastructure`, Quarkus
- `shared/build.gradle.kts` — standalone utilities, minimal dependencies
- `infrastructure/build.gradle.kts` — DB drivers, shared config
- `ui/web/package.json` — React, npm scripts for generation

---

## Database Migrations — Flyway

Migrations run automatically on Quarkus startup.

```text
db/migration/
├── V1__create_banking_transaction_table.sql    (Finance)
├── V2__create_investment_transaction_table.sql (Finance)
├── V3__create_health_profile_table.sql         (Health)
├── V4__create_doctor_visit_table.sql           (Health)
└── V5__create_vital_reading_table.sql          (Health)
```

**Flyway rules (never break these):**
- Never edit a migration file that has already run
- Always add a new file for schema changes
- Version numbers must be sequential

---

## Configuration — application.properties

Each domain has its own `application.properties`:

```properties
# Database
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/app_db
quarkus.datasource.username=${DB_USER}
quarkus.datasource.password=${DB_PASSWORD}

# Finance domain
app.finance.account-names=HDFC_SAVINGS,ICICI_CREDIT_CARD,SBI_LOAN
app.finance.fund-names=PF,NPS,LARGE_CAP,MID_CAP

# Port
quarkus.http.port=8080
```

**Key principle:** Valid account/fund names are ENUM-driven from config, not hardcoded. Adding a new account only requires restarting with updated `application.properties`.

---

## Testing Strategy

(To be defined — add test structure as project grows)

Recommended:
- **Unit tests** for `domain/` logic (AccountType validation, amount normalization)
- **Integration tests** for `application/` services (parse → dedup → persist workflows)
- **Contract tests** for `adapters/in/http/` controllers (ensure responses match OpenAPI)
- **End-to-end tests** for full upload flow with real PostgreSQL

---

## Design Decisions — Why Hexagonal?

| Decision | Rationale |
|---|---|
| **Hexagonal architecture** | Pure domain logic decoupled from framework. Easy to test, refactor, swap implementations (e.g., replace Panache with plain JDBC). |
| **Contract-first OpenAPI** | API is the contract. Both backend and frontend work from the same spec. Reduces mismatches. |
| **Single database** | Simpler for Phase 1. Each domain owns its tables. Phase 3 will consider multi-tenancy. |
| **Configuration-driven ENUMs** | Adding a new account doesn't require code recompile — just update `application.properties` and restart. |
| **Duplicate flagging (not rejection)** | V1 prioritizes not losing data. Phase 2 adds accept/reject UI for marked duplicates. |
| **No file persistence** | Simpler, privacy-respecting, no storage costs. Files parsed in memory. |
| **Google AIP style** | Industry standard for resource-oriented APIs. Consistent with Cloud APIs conventions. |
| **Single Quarkus application** | One JVM process for both Finance and Health domains. Package boundaries enforce hexagonal isolation. Splitting into separate services is a one-afternoon refactor if Phase 3 requires it. |

---

## Glossary

- **Hexagon:** A domain bounded by ports/adapters. Core business logic insulated from external concerns.
- **Port:** Interface between domain and outside world. Two types: driving (use cases), driven (repositories).
- **Adapter:** Implementation of a port. Examples: REST controller (driving), Panache repository (driven).
- **Domain entity:** Pure Java object representing a business concept (Transaction, Account). No framework annotations.
- **Use case:** A feature or workflow. Interface in `ports/in/`, implementation in `application/`.
- **Repository:** Persistence abstraction. Interface in `ports/out/`, implementation in `adapters/out/`.
- **Deduplication:** Detecting overlapping uploads. Composite UNIQUE constraint + `is_duplicate` flag.

---

## Next Steps

- See [Getting Started](../GETTING_STARTED.md) to run locally
- See [Business Requirement](./Business_Requirement.md) for API specs and data model
- See [Roadmap](./ROADMAP.md) for Phase 2–4 plans
