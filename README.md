# Suchika (सूचिका)

A personal data record management system managing **Finance** and **Health** domains with a React frontend.

Built on **Hexagonal Architecture** (Ports & Adapters). Currently in **Phase 1 (V1)** — Finance CSV upload workflow.

---

## 🚀 Quick Start

1. **Clone & setup:** See [Getting Started](./documents/GETTING_STARTED.md)
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
| [GETTING_STARTED](./documents/GETTING_STARTED.md) | Local dev setup, prerequisites, run commands |
| [ARCHITECTURE](./documents/ARCHITECTURE.md) | System design and hexagonal architecture |
| [BUSINESS_REQUIREMENTS](./documents/BUSINESS_REQUIREMENTS.md) | Functional specs, API contracts, data model |
| [CICD](./documents/CICD.md) | Build and automation pipeline rules |
| [AGENTS](./documents/AGENTS.md) | AI helper roles and documentation agents |
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
├── .github/
│   └── copilot/
│       └── agents/
├── .husky/
│   └── pre-commit
├── application/
│   └── finance/
│       ├── build.gradle.kts
│       └── src/main/
│           ├── java/com/suchika/finance/
│           ├── java/com/suchika/health/
│           └── resources/
│               └── db/migration/
├── documents/
│   ├── AGENTS.md
│   ├── API.md
│   ├── ARCHITECTURE.md
│   ├── BUSINESS_REQUIREMENTS.md
│   ├── CICD.md
│   ├── GETTING_STARTED.md
│   ├── ROADMAP.md
│   └── instructetions.md
├── infrastructure/
├── openapi/
│   ├── finance.yaml
│   └── health.yaml
├── shared/
├── web/
│   ├── package.json
│   ├── src/
│   └── public/
├── build.gradle.kts
├── gradlew
├── gradlew.bat
├── package-lock.json
├── README.md
└── settings.gradle.kts
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

See [BUSINESS_REQUIREMENTS](./documents/BUSINESS_REQUIREMENTS.md) for full API spec.

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

1. Read [ARCHITECTURE](./documents/ARCHITECTURE.md) to understand the design
2. Follow Hexagonal Architecture rules (domain is framework-free)
3. Keep migrations sequential (never edit a run migration)
4. Run tests before committing

---

## 📞 Support

- **Setup issues?** → [Getting Started](./documents/GETTING_STARTED.md)
- **How to use?** → [GETTING_STARTED](./documents/GETTING_STARTED.md)
- **API details?** → [BUSINESS_REQUIREMENTS](./documents/BUSINESS_REQUIREMENTS.md)
- **Architecture?** → [ARCHITECTURE](./documents/ARCHITECTURE.md)
