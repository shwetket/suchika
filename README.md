# Suchika (सूचिका)

A personal data record management system managing **Wealth**, **Household**, and **Health** domains with a React frontend.

Built on **Hexagonal Architecture** (Ports & Adapters). Currently at **v0.1** — Wealth CSV upload workflow (happy path).

---

## 🚀 Quick Start

1. **Clone & setup:** See [Contributing / Getting Started](./CONTRIBUTING.md)
2. **Run the unified backend:**
```bash
   ./gradlew :application:finance:quarkusDev   # Single Quarkus application
   cd web && npm install && npm start          # Frontend
```
3. **Open app:** `http://localhost:3000`

---

## 📖 Documentation

| Document | Purpose |
|---|---|
| [CONTRIBUTING](./CONTRIBUTING.md) | Local dev setup, prerequisites, run commands |
| [ARCHITECTURE](./ARCHITECTURE.md) | System design and hexagonal architecture |
| [BUSINESS_REQUIREMENTS](./BUSINESS_REQUIREMENTS.md) | Functional specs, versioned epics, domain rules |
| [CICD](./CICD.md) | Build and automation pipeline rules |
| [AGENTS](./AGENTS.md) | AI helper roles and documentation agents |
| [ROADMAP](./ROADMAP.md) | Future milestones v0.2 → v4.1 |
| [SECURITY](./SECURITY.md) | Vulnerability reporting and version support |

---

## 🛠 Tech Stack

| Layer | Technology |
|---|---|
| Backend Language | Java 25 |
| Backend Framework | Quarkus 3.29.0 |
| Build Tool | Gradle 9.3.0 (Kotlin DSL) |
| Relational DB | PostgreSQL (Wealth + Household domains) |
| Document DB | MongoDB (Health domain) |
| Schema Migrations | Flyway (PostgreSQL) |
| API Contract | OpenAPI 3.1.0 (contract-first) |
| API Style | Google AIP (resource-oriented) |
| Frontend | React (JavaScript) |
| Architecture | Hexagonal (Ports & Adapters) |

---

## 📁 Repository Structure
suchika/
├── .github/
│   └── copilot/
│       └── agents/
├── .husky/
│   └── pre-commit
├── application/
│   ├── finance/          # Wealth domain backend
│   ├── health/           # Health domain backend
│   └── records/          # Household domain backend
├── documents/
│   ├── records/
│   │   ├── wealth_domain.md
│   │   ├── household_domain.md
│   │   ├── health_domain.md
│   │   └── cross_domain.md
│   ├── AGENTS.md
│   ├── ARCHITECTURE.md
│   ├── BUSINESS_REQUIREMENTS.md
│   ├── CICD.md
│   ├── CONTRIBUTING.md
│   ├── ROADMAP.md
│   └── SECURITY.md
├── infrastructure/
├── openapi/
│   ├── finance.yaml
│   ├── health.yaml
│   └── household.yaml
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

---

## 🏗 Architecture Overview

Hexagonal architecture with clear separation of concerns across three isolated domains:
┌──── DOMAIN ────┐
HTTP   → │ ports/in       │ → DB
│ application    │
│   ↓            │
│ ports/out      │
└────────────────┘
↑        ↑
adapters/   adapters/
in (HTTP)   out (DB)
Domains:  wealth | household | health
DBs:      PostgreSQL (wealth, household) | MongoDB (health)
**Key rule:** Domain logic is framework-agnostic and fully testable. No dependencies on Quarkus, Panache, or external libraries inside `domain/`.

---

## 💾 Database

| Domain | DB | Key Tables / Collections |
|---|---|---|
| Wealth | PostgreSQL (`app_db`) | `banking_transaction`, `investment_transaction`, `vehicle_asset` |
| Household | PostgreSQL (`app_db`) | `household_profile`, `calendar_event`, `inventory_item` |
| Health | MongoDB | `biometric_entry`, `health_profile` |

Each domain owns its tables — no cross-domain joins.

---

## 🔗 API

**Base path:** `/api/v1`

| Domain | Endpoint | Method | Purpose |
|---|---|---|---|
| Wealth | `/transactions:uploadCsv` | POST | Upload CSV file |
| Wealth | `/transactions` | GET | List transactions |
| Wealth | `/transactions:config` | GET | Get dropdown values |
| Wealth | `/accounts` | GET/POST | Manage accounts |
| Health | `/health-profiles` | GET/POST | Manage health profiles |
| Household | *(coming in v0.1+)* | — | Profiles, calendar, inventory |

See [ARCHITECTURE](./ARCHITECTURE.md) for full API spec.

---

## 📍 Current Milestone: v0.1 — Wealth CSV Upload

**In scope:**
- Upload banking & investment transactions from CSV
- Automatic deduplication (flag cross-file duplicates)
- Configuration-driven account names
- Manual biometric logging (Health)
- Household profile registry and basic calendar

**Not in v0.1:**
- Cross-domain logic (deferred to v0.5)
- Auth / encryption (deferred to v1.0)
- Error handling for malformed data (deferred to v0.4)
- External API integrations (deferred to v1.0+)

See [ROADMAP](./ROADMAP.md) and [BUSINESS_REQUIREMENTS](./BUSINESS_REQUIREMENTS.md) for full milestone plan.

---

## 🤝 Contributing

1. Read [ARCHITECTURE](./ARCHITECTURE.md) to understand the design
2. Follow Hexagonal Architecture rules (domain is framework-free)
3. Keep migrations sequential (never edit a committed migration)
4. Run tests before committing

See [CONTRIBUTING](./CONTRIBUTING.md) for full setup instructions.

---

## 📞 Support

- **Setup issues?** → [CONTRIBUTING](./CONTRIBUTING.md)
- **API details?** → [ARCHITECTURE](./ARCHITECTURE.md)
- **Business rules?** → [BUSINESS_REQUIREMENTS](./BUSINESS_REQUIREMENTS.md)
- **Security issues?** → [SECURITY](./SECURITY.md)