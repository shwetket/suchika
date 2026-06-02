# Suchika (सूचिका)

A personal data record management system managing **Wealth**, **Household**, and **Health** domains with a React frontend.

Built on **Hexagonal Architecture** (Ports & Adapters). Currently at **v0.1** — Wealth CSV upload workflow (happy path).

---

## 🚀 Quick Start

1. **Clone & setup:** See [Contributing / Getting Started](./CONTRIBUTING.md)
2. **Run the unified backend:**
```bash
   ./gradlew :application:wealth:quarkusDev   # Single Quarkus application
   cd web && npm install && npm start          # Frontend
```
3. **Open app:** `http://localhost:3000`

---

## 📖 Documentation

| Document | Purpose |
|---|---|
| [CONTRIBUTING](./CONTRIBUTING.md) | Local dev setup, prerequisites, run commands |
| [ARCHITECTURE](./documents/ARCHITECTURE_GUIDELINES.md) | System design and hexagonal architecture |
| [FRONTEND_GUIDELINES](./documents/FRONTEND_GUIDELINES.md) | React standards, guardrails, and linting rules |
| [BUSINESS_REQUIREMENTS](./documents/BUSINESS_REQUIREMENTS.md) | Functional specs, versioned epics, domain rules |
| [CICD](./documents/CICD.md) | Build and automation pipeline rules |
| [LOGGING_AND_EXCEPTIONS](./documents/LOGGING_AND_EXCEPTIONS.md) | Logger utility and common exception handling |
| [AGENTS](./documents/AGENTS.md) | AI helper roles and documentation agents |
| [ROADMAP](./documents/ROADMAP.md) | Future milestones v0.2 → v4.1 |
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



```
suchika/
├── .husky/
│   └── pre-commit
├── application/
│   ├── contract/
│   │   ├── gateway.yaml
│   │   ├── health.yaml
│   │   ├── household.yaml
│   │   └── wealth.yaml
│   ├── domain/
│   │   ├── health/
│   │   │   ├── adapters/
│   │   │   │   ├── src/
│   │   │   │   │   ├── main/
│   │   │   │   │   │   ├── java/
│   │   │   │   │   │   │   └── com/
│   │   │   │   │   │   │       └── suchika/
│   │   │   │   │   │   │           └── health/
│   │   │   │   │   │   │               └── adapters/
│   │   │   │   │   │   │                   └── HealthApplication.java
│   │   │   │   │   │   └── resources/
│   │   │   │   │   │       └── application.properties
│   │   │   │   │   └── test/
│   │   │   │   │       └── java/
│   │   │   │   │           └── com/
│   │   │   │   │               └── suchika/
│   │   │   │   │                   └── health/
│   │   │   │   │                       └── adapters/
│   │   │   │   └── build.gradle.kts
│   │   │   ├── domain/
│   │   │   │   ├── src/
│   │   │   │   │   ├── main/
│   │   │   │   │   │   ├── java/
│   │   │   │   │   │   │   └── com/
│   │   │   │   │   │   │       └── suchika/
│   │   │   │   │   │   │           └── health/
│   │   │   │   │   │   │               └── domain/
│   │   │   │   │   │   └── resources/
│   │   │   │   │   └── test/
│   │   │   │   │       └── java/
│   │   │   │   │           └── com/
│   │   │   │   │               └── suchika/
│   │   │   │   │                   └── health/
│   │   │   │   │                       └── domain/
│   │   │   │   └── build.gradle.kts
│   │   │   ├── ports/
│   │   │   │   ├── src/
│   │   │   │   │   ├── main/
│   │   │   │   │   │   ├── java/
│   │   │   │   │   │   │   └── com/
│   │   │   │   │   │   │       └── suchika/
│   │   │   │   │   │   │           └── health/
│   │   │   │   │   │   │               └── ports/
│   │   │   │   │   │   └── resources/
│   │   │   │   │   └── test/
│   │   │   │   │       └── java/
│   │   │   │   │           └── com/
│   │   │   │   │               └── suchika/
│   │   │   │   │                   └── health/
│   │   │   │   │                       └── ports/
│   │   │   │   └── build.gradle.kts
│   │   │   └── src/
│   │   │       ├── main/
│   │   │       │   ├── java/
│   │   │       │   │   └── com/
│   │   │       │   │       └── suchika/
│   │   │       │   │           └── health/
│   │   │       │   └── resources/
│   │   │       └── test/
│   │   │           └── java/
│   │   │               └── com/
│   │   │                   └── suchika/
│   │   │                       └── health/
│   │   ├── household/
│   │   │   ├── adapters/
│   │   │   │   ├── src/
│   │   │   │   │   └── main/
│   │   │   │   │       ├── java/
│   │   │   │   │       └── resources/
│   │   │   │   │           ├── db/
│   │   │   │   │           └── application.properties
│   │   │   │   └── build.gradle.kts
│   │   │   ├── domain/
│   │   │   │   ├── src/
│   │   │   │   │   ├── main/
│   │   │   │   │   │   └── java/
│   │   │   │   │   └── test/
│   │   │   │   │       └── java/
│   │   │   │   └── build.gradle.kts
│   │   │   ├── ports/
│   │   │   │   ├── src/
│   │   │   │   │   ├── main/
│   │   │   │   │   │   └── java/
│   │   │   │   │   └── test/
│   │   │   │   │       └── java/
│   │   │   │   └── build.gradle.kts
│   │   │   ├── src/
│   │   │   │   └── main/
│   │   │   │       └── resources/
│   │   │   │           └── db/
│   │   │   │               └── migration/
│   │   │   └── build.gradle.kts
│   │   └── wealth/
│   │       ├── adapters/
│   │       │   ├── src/
│   │       │   │   ├── main/
│   │       │   │   │   ├── java/
│   │       │   │   │   │   └── com/
│   │       │   │   │   │       └── suchika/
│   │       │   │   │   │           ├── finance/
│   │       │   │   │   │           │   └── adapters/
│   │       │   │   │   │           └── wealth/
│   │       │   │   │   │               └── adapters/
│   │       │   │   │   │                   └── WealthApplication.java
│   │       │   │   │   └── resources/
│   │       │   │   │       ├── db/
│   │       │   │   │       │   └── migration/
│   │       │   │   │       └── application.properties
│   │       │   │   └── test/
│   │       │   │       └── java/
│   │       │   │           └── com/
│   │       │   │               └── suchika/
│   │       │   │                   └── finance/
│   │       │   │                       └── adapters/
│   │       │   └── build.gradle.kts
│   │       ├── domain/
│   │       │   ├── src/
│   │       │   │   ├── main/
│   │       │   │   │   ├── java/
│   │       │   │   │   │   └── com/
│   │       │   │   │   │       └── suchika/
│   │       │   │   │   │           └── finance/
│   │       │   │   │   │               └── domain/
│   │       │   │   │   └── resources/
│   │       │   │   └── test/
│   │       │   │       └── java/
│   │       │   │           └── com/
│   │       │   │               └── suchika/
│   │       │   │                   └── finance/
│   │       │   │                       └── domain/
│   │       │   └── build.gradle.kts
│   │       ├── ports/
│   │       │   ├── src/
│   │       │   │   ├── main/
│   │       │   │   │   ├── java/
│   │       │   │   │   │   └── com/
│   │       │   │   │   │       └── suchika/
│   │       │   │   │   │           └── finance/
│   │       │   │   │   │               └── ports/
│   │       │   │   │   └── resources/
│   │       │   │   └── test/
│   │       │   │       └── java/
│   │       │   │           └── com/
│   │       │   │               └── suchika/
│   │       │   │                   └── finance/
│   │       │   │                       └── ports/
│   │       │   └── build.gradle.kts
│   │       └── src/
│   │           ├── main/
│   │           │   ├── java/
│   │           │   │   └── com/
│   │           │   │       └── suchika/
│   │           │   │           └── finance/
│   │           │   └── resources/
│   │           └── test/
│   │               └── java/
│   │                   └── com/
│   │                       └── suchika/
│   │                           └── finance/
│   ├── flyway/
│   │   ├── health/
│   │   │   ├── V4__create_health_profile_table.sql
│   │   │   ├── V5__create_doctor_visit_table.sql
│   │   │   └── V6__create_vital_reading_table.sql
│   │   ├── household/
│   │   │   └── V3__create_goal_table.sql
│   │   └── wealth/
│   │       ├── V1__create_account_table.sql
│   │       ├── V2__create_transaction_table.sql
│   │       └── V7__add_jsonb_to_transaction.sql
│   └── web-gateway/
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/
│       │   │   │   └── com/
│       │   │   │       └── suchika/
│       │   │   │           └── gateway/
│       │   │   └── resources/
│       │   │       ├── health.yaml
│       │   │       └── wealth.yaml
│       │   └── test/
│       │       └── java/
│       │           └── com/
│       │               └── suchika/
│       │                   └── gateway/
│       └── build.gradle.kts
├── assets/
│   └── images/
├── documents/
│   ├── temp/
│   ├── AGENTS.md
│   ├── ARCHITECTURE_DECISIONS.md
│   ├── ARCHITECTURE_GUIDELINES.md
│   ├── ARCHITECTURE_PROPOSALS.md
│   ├── BUSINESS_REQUIREMENTS.md
│   ├── CICD.md
│   ├── FRONTEND_GUIDELINES.md
│   ├── LOGGING_AND_EXCEPTIONS.md
│   ├── REQUIREMENTS_cross_domain.md
│   ├── REQUIREMENTS_health_domain.md
│   ├── REQUIREMENTS_household_domain.md
│   ├── REQUIREMENTS_wealth_domain.md
│   └── ROADMAP.md
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── infrastructure/
│   ├── local/
│   │   └── .env.template
│   └── build.gradle.kts
├── scripts/
│   ├── check-migrations-location.sh
│   └── documentWriter.py
├── shared/
│   ├── src/
│   │   ├── main/
│   │   │   └── java/
│   │   │       └── com/
│   │   │           └── suchika/
│   │   │               └── shared/
│   │   │                   ├── dto/
│   │   │                   │   └── ErrorResponse.java
│   │   │                   ├── exception/
│   │   │                   │   ├── ApplicationException.java
│   │   │                   │   ├── BadRequestException.java
│   │   │                   │   ├── ConflictException.java
│   │   │                   │   ├── ForbiddenException.java
│   │   │                   │   ├── InternalServerException.java
│   │   │                   │   ├── NotAcceptableException.java
│   │   │                   │   ├── NotFoundException.java
│   │   │                   │   ├── NotImplementedException.java
│   │   │                   │   └── UnauthorizedException.java
│   │   │                   ├── logging/
│   │   │                   │   └── AppLogger.java
│   │   │                   └── mapper/
│   │   │                       └── ApplicationExceptionMapper.java
│   │   └── test/
│   │       └── java/
│   │           └── com/
│   │               └── suchika/
│   │                   └── architecture/
│   │                       └── DomainRulesTest.java
│   └── build.gradle.kts
├── web/
│   ├── public/
│   │   ├── css/
│   │   │   └── style.css
│   │   ├── html/
│   │   │   ├── about.html
│   │   │   ├── projects.html
│   │   │   └── skill.html
│   │   ├── images/
│   │   ├── favicon.ico
│   │   ├── index.html
│   │   ├── manifest.json
│   │   ├── robots.txt
│   │   └── tailwind.config.js
│   ├── resources/
│   │   ├── darkmode.png
│   │   ├── darkmode.psd
│   │   ├── github-logo.png
│   │   ├── hackerrank_logo.png
│   │   ├── lightmode.png
│   │   ├── linkedin-logo.png
│   │   ├── logo.svg
│   │   ├── logo192.png
│   │   ├── logo512.png
│   │   ├── map.png
│   │   └── WhatsApp-logo.png
│   ├── scripts/
│   │   └── copy-assets.js
│   ├── src/
│   │   ├── api/
│   │   │   ├── authApi.js
│   │   │   ├── client.js
│   │   │   ├── generated.d.ts
│   │   │   ├── generated.ts
│   │   │   └── healthApi.js
│   │   ├── components/
│   │   │   ├── Navigation.js
│   │   │   └── ProtectedRoute.js
│   │   ├── context/
│   │   │   └── AuthContext.js
│   │   ├── hooks/
│   │   │   └── useAuth.js
│   │   ├── pages/
│   │   │   ├── Admin/
│   │   │   │   ├── AdminReports.js
│   │   │   │   ├── AdminSettings.js
│   │   │   │   └── AdminUsers.js
│   │   │   ├── Public/
│   │   │   │   ├── About.js
│   │   │   │   ├── Home.js
│   │   │   │   ├── SignIn.js
│   │   │   │   └── SignUp.js
│   │   │   └── User/
│   │   │       ├── Dashboard.js
│   │   │       ├── Health.js
│   │   │       └── Transactions.js
│   │   ├── App.css
│   │   ├── App.js
│   │   ├── App.test.js
│   │   ├── index.css
│   │   ├── index.js
│   │   ├── reportWebVitals.js
│   │   └── setupTests.js
│   ├── .eslintrc.json
│   ├── .gitignore
│   ├── .prettierignore
│   ├── .prettierrc.json
│   ├── package-lock.json
│   └── package.json
├── .gitignore
├── build.gradle.kts
├── CODE_OF_CONDUCT.md
├── CONTRIBUTING.md
├── gradlew
├── gradlew.bat
├── LICENSE
├── package-lock.json
├── README.md
├── SECURITY.md
└── settings.gradle.kts
```




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

See [ARCHITECTURE](./documents/ARCHITECTURE_GUIDELINES.md) for full API spec.

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

See [ROADMAP](./documents/ROADMAP.md) and [BUSINESS_REQUIREMENTS](./documents/BUSINESS_REQUIREMENTS.md) for full milestone plan.

---

## 🤝 Contributing

1. Read [ARCHITECTURE](./documents/ARCHITECTURE_GUIDELINES.md) to understand the design
2. Follow Hexagonal Architecture rules (domain is framework-free)
3. Keep migrations sequential (never edit a committed migration)
4. Run tests before committing

See [CONTRIBUTING](./CONTRIBUTING.md) for full setup instructions.

---

## 📞 Support

- **Setup issues?** → [CONTRIBUTING](./CONTRIBUTING.md)
- **API details?** → [ARCHITECTURE](./documents/ARCHITECTURE_GUIDELINES.md)
- **Business rules?** → [BUSINESS_REQUIREMENTS](./documents/BUSINESS_REQUIREMENTS.md)
- **Security issues?** → [SECURITY](./SECURITY.md)