# Suchika (सूचिका)

A personal data record management system managing **Finance** and **Health** domains with a React frontend.

Built on **Hexagonal Architecture** (Ports & Adapters). Currently in **Phase 1 (V1)** — Finance CSV upload workflow.

---

## 🚀 Quick Start

1. **Clone & setup:** See [Getting Started](./GETTING_STARTED.md)
2. **Run the unified backend:**
   ```bash
   ./gradlew :application:finance:quarkusDev   # Single Quarkus application
   cd web && npm install && npm start         # Frontend
   ```
3. **Open app:** `http://localhost:3000`

---

## 📖 Documentation

| Document | Purpose |
|---|---|
| [Getting Started](./GETTING_STARTED.md) | Local dev setup, prerequisites, run commands |
| [User Guide](./documents/User_Guide.md) | How to upload CSVs and use the app |
| [Business Requirement](./documents/Business_Requirement.md) | Functional specs, API contracts, data model |
| [Project Architecture](./documents/Project_Architecture.md) | Hexagonal design, file structure, design decisions |
| [Roadmap](./documents/ROADMAP.md) | Phases 2–4 planning |

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Backend Language | Java 25 |
| Backend Framework | Quarkus 3.29.0 |
| Build Tool | Gradle 9.3.0 (Kotlin DSL) |
| Database | PostgreSQL (single `app_db`) |
| Schema Migrations | Flyway |
| API Contract | OpenAPI 3.1.0 (contract-first) |
| API Style | Google AIP (resource-oriented) |
| Frontend | React (JavaScript) |
| Architecture | Hexagonal (Ports & Adapters) |

---

## 📁 Repository Structure
```
suchika/
├── application/                  # Unified backend
│   ├── finance/                  # Single Quarkus application module
│   │   ├── build.gradle.kts
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
│   │           ├── application.properties
│   │           └── db/migration/
│   │               ├── V1__create_account_table.sql
│   │               ├── V2__create_transaction_table.sql
│   │               ├── V3__create_goal_table.sql
│   │               ├── V4__create_health_profile_table.sql
│   │               ├── V5__create_doctor_visit_table.sql
│   │               └── V6__create_vital_reading_table.sql
├── infrastructure/               # Shared plumbing (DB connections, config)
├── shared/                       # Cross-cutting concerns (auth, logging, errors)
├── openapi/                      # OpenAPI contracts
│   ├── finance.yaml
│   └── health.yaml
├── web/                          # React frontend
│   ├── src/
│   │   ├── App.js                # Main component
│   │   └── api/generated/        # Auto-generated API clients
│   └── package.json
├── .husky/                       # Git hooks
│   └── pre-commit
├── README.md                     # This file
└── documents/
    ├── API.md
    ├── Business_Requirement.md
    ├── CONTRIBUTING.md
    ├── GETTING_STARTED.md
    ├── Project_Architecture.md
    ├── ROADMAP.md
    ├── User_Guide.md
    └── web_README.md
```

---

## 🏗 Architecture Overview

Hexagonal architecture with clear separation of concerns:

```
            ┌──── DOMAIN ────┐
   HTTP   → │ ports/in       │ → DB
            │ application    │
            │   ↓            │
            │ ports/out      │
            └────────────────┘
                ↑        ↑
            adapters/   adapters/
            in (HTTP)   out (DB)
```

**Key rule:** Domain logic is framework-agnostic and fully testable. No dependencies on Quarkus, Panache, or external libraries inside `domain/`.

---

## 💾 Database

Single PostgreSQL instance (`app_db`) shared by all domains:

- **Finance tables:** `banking_transaction`, `investment_transaction`
- **Health tables:** `health_profile`, `doctor_visit`, `vital_reading`
- **Migrations:** Managed by Flyway (sequential versions V1–V6)

Each domain owns its tables — no cross-domain joins.

---

## 🔗 API

**Base path:** `/api/v1`

| Endpoint | Method | Purpose |
|---|---|---|
| `/transactions:uploadCsv` | POST | Upload CSV file |
| `/transactions` | GET | List transactions with pagination |
| `/transactions:config` | GET | Get dropdown values |

See [Business Requirement](./documents/Business_Requirement.md) for full API spec.

---

## 📝 Phase 1 (V1) — Finance CSV Upload

**Current focus:**
- Upload banking & investment transactions from CSV
- Automatic deduplication (flag duplicates, don't drop)
- Configuration-driven account names
- Simple, no AI, no categorization

**Not in V1:**
- Duplicate accept/reject UI (Phase 2)
- Multi-user / auth (Phase 3)
- Transfer reconciliation (Phase 2)
- Analytics / dashboard (Phase 3+)

See [Roadmap](./documents/ROADMAP.md) for phases 2–4.

---

## 🤝 Contributing

1. Read [Project Architecture](./documents/Project_Architecture.md) to understand the design
2. Follow Hexagonal Architecture rules (domain is framework-free)
3. Keep migrations sequential (never edit a run migration)
4. Run tests before committing

---

## 📞 Support

- **Setup issues?** → [Getting Started](./GETTING_STARTED.md)
- **How to use?** → [User Guide](./documents/User_Guide.md)
- **API details?** → [Business Requirement](./documents/Business_Requirement.md)
- **Architecture?** → [Project Architecture](./documents/Project_Architecture.md)
