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
| Code Quality | SonarQube Community Edition (local analysis) |

---

## 📁 Repository Structure






```
suchika/
├── application/
│   ├── contract/
│   │   ├── gateway.yaml
│   │   ├── health.yaml
│   │   ├── household.yaml
│   │   ├── profile.yaml
│   │   └── wealth.yaml
│   ├── domain/
│   │   ├── health/
│   │   │   ├── adapters/
│   │   │   │   ├── bin/
│   │   │   │   │   ├── main/
│   │   │   │   │   │   └── com/
│   │   │   │   │   │       └── suchika/
│   │   │   │   │   │           └── health/
│   │   │   │   │   │               └── adapters/
│   │   │   │   │   │                   ├── http/
│   │   │   │   │   │                   │   └── dto/
│   │   │   │   │   │                   ├── persistence/
│   │   │   │   │   │                   └── services/
│   │   │   │   │   └── test/
│   │   │   │   │       └── com/
│   │   │   │   │           └── suchika/
│   │   │   │   │               └── health/
│   │   │   │   │                   └── adapters/
│   │   │   │   │                       └── services/
│   │   │   │   ├── src/
│   │   │   │   │   ├── main/
│   │   │   │   │   │   ├── java/
│   │   │   │   │   │   │   └── com/
│   │   │   │   │   │   │       └── suchika/
│   │   │   │   │   │   │           └── health/
│   │   │   │   │   │   │               └── adapters/
│   │   │   │   │   │   │                   ├── http/
│   │   │   │   │   │   │                   │   └── dto/
│   │   │   │   │   │   │                   ├── persistence/
│   │   │   │   │   │   │                   └── services/
│   │   │   │   │   │   └── resources/
│   │   │   │   │   │       └── application.properties
│   │   │   │   │   └── test/
│   │   │   │   │       └── java/
│   │   │   │   │           └── com/
│   │   │   │   │               └── suchika/
│   │   │   │   │                   └── health/
│   │   │   │   │                       └── adapters/
│   │   │   │   │                           └── services/
│   │   │   │   └── build.gradle.kts
│   │   │   ├── bin/
│   │   │   │   ├── main/
│   │   │   │   │   └── com/
│   │   │   │   │       └── suchika/
│   │   │   │   │           └── health/
│   │   │   │   └── test/
│   │   │   │       └── com/
│   │   │   │           └── suchika/
│   │   │   │               └── health/
│   │   │   ├── domain/
│   │   │   │   ├── bin/
│   │   │   │   │   ├── main/
│   │   │   │   │   │   └── com/
│   │   │   │   │   │       └── suchika/
│   │   │   │   │   │           └── health/
│   │   │   │   │   │               └── domain/
│   │   │   │   │   └── test/
│   │   │   │   │       └── com/
│   │   │   │   │           └── suchika/
│   │   │   │   │               └── health/
│   │   │   │   │                   └── domain/
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
│   │   │   │   ├── bin/
│   │   │   │   │   ├── main/
│   │   │   │   │   │   └── com/
│   │   │   │   │   │       └── suchika/
│   │   │   │   │   │           └── health/
│   │   │   │   │   │               └── ports/
│   │   │   │   │   │                   ├── input/
│   │   │   │   │   │                   └── output/
│   │   │   │   │   └── test/
│   │   │   │   │       └── com/
│   │   │   │   │           └── suchika/
│   │   │   │   │               └── health/
│   │   │   │   │                   └── ports/
│   │   │   │   ├── src/
│   │   │   │   │   ├── main/
│   │   │   │   │   │   ├── java/
│   │   │   │   │   │   │   └── com/
│   │   │   │   │   │   │       └── suchika/
│   │   │   │   │   │   │           └── health/
│   │   │   │   │   │   │               └── ports/
│   │   │   │   │   │   │                   ├── input/
│   │   │   │   │   │   │                   └── output/
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
│   │   │   │   ├── bin/
│   │   │   │   │   └── main/
│   │   │   │   ├── src/
│   │   │   │   │   └── main/
│   │   │   │   │       ├── java/
│   │   │   │   │       └── resources/
│   │   │   │   │           ├── db/
│   │   │   │   │           └── application.properties
│   │   │   │   └── build.gradle.kts
│   │   │   ├── bin/
│   │   │   │   └── main/
│   │   │   ├── domain/
│   │   │   │   ├── bin/
│   │   │   │   │   ├── main/
│   │   │   │   │   └── test/
│   │   │   │   ├── src/
│   │   │   │   │   ├── main/
│   │   │   │   │   │   └── java/
│   │   │   │   │   └── test/
│   │   │   │   │       └── java/
│   │   │   │   └── build.gradle.kts
│   │   │   ├── ports/
│   │   │   │   ├── bin/
│   │   │   │   │   ├── main/
│   │   │   │   │   └── test/
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
│   │   ├── profile/
│   │   │   ├── adapters/
│   │   │   │   ├── bin/
│   │   │   │   │   ├── main/
│   │   │   │   │   │   └── com/
│   │   │   │   │   │       └── suchika/
│   │   │   │   │   │           └── profile/
│   │   │   │   │   │               └── adapters/
│   │   │   │   │   │                   ├── http/
│   │   │   │   │   │                   │   └── dto/
│   │   │   │   │   │                   ├── persistence/
│   │   │   │   │   │                   └── service/
│   │   │   │   │   └── test/
│   │   │   │   │       └── com/
│   │   │   │   │           └── suchika/
│   │   │   │   │               └── profile/
│   │   │   │   │                   └── adapters/
│   │   │   │   │                       └── service/
│   │   │   │   ├── src/
│   │   │   │   │   ├── main/
│   │   │   │   │   │   ├── java/
│   │   │   │   │   │   │   └── com/
│   │   │   │   │   │   │       └── suchika/
│   │   │   │   │   │   │           └── profile/
│   │   │   │   │   │   │               └── adapters/
│   │   │   │   │   │   │                   ├── http/
│   │   │   │   │   │   │                   │   └── dto/
│   │   │   │   │   │   │                   ├── persistence/
│   │   │   │   │   │   │                   └── service/
│   │   │   │   │   │   └── resources/
│   │   │   │   │   │       ├── db/
│   │   │   │   │   │       └── application.properties
│   │   │   │   │   └── test/
│   │   │   │   │       └── java/
│   │   │   │   │           └── com/
│   │   │   │   │               └── suchika/
│   │   │   │   │                   └── profile/
│   │   │   │   │                       └── adapters/
│   │   │   │   │                           └── service/
│   │   │   │   └── build.gradle.kts
│   │   │   ├── bin/
│   │   │   │   └── main/
│   │   │   ├── domain/
│   │   │   │   ├── bin/
│   │   │   │   │   ├── main/
│   │   │   │   │   │   └── com/
│   │   │   │   │   │       └── suchika/
│   │   │   │   │   │           └── profile/
│   │   │   │   │   │               └── domain/
│   │   │   │   │   └── test/
│   │   │   │   │       └── com/
│   │   │   │   │           └── suchika/
│   │   │   │   │               └── profile/
│   │   │   │   │                   └── domain/
│   │   │   │   ├── src/
│   │   │   │   │   ├── main/
│   │   │   │   │   │   └── java/
│   │   │   │   │   │       └── com/
│   │   │   │   │   │           └── suchika/
│   │   │   │   │   │               └── profile/
│   │   │   │   │   │                   └── domain/
│   │   │   │   │   └── test/
│   │   │   │   │       └── java/
│   │   │   │   │           └── com/
│   │   │   │   │               └── suchika/
│   │   │   │   │                   └── profile/
│   │   │   │   │                       └── domain/
│   │   │   │   └── build.gradle.kts
│   │   │   ├── ports/
│   │   │   │   ├── bin/
│   │   │   │   │   ├── main/
│   │   │   │   │   │   └── com/
│   │   │   │   │   │       └── suchika/
│   │   │   │   │   │           └── profile/
│   │   │   │   │   │               └── ports/
│   │   │   │   │   │                   ├── input/
│   │   │   │   │   │                   └── output/
│   │   │   │   │   └── test/
│   │   │   │   ├── src/
│   │   │   │   │   ├── main/
│   │   │   │   │   │   └── java/
│   │   │   │   │   │       └── com/
│   │   │   │   │   │           └── suchika/
│   │   │   │   │   │               └── profile/
│   │   │   │   │   │                   └── ports/
│   │   │   │   │   │                       ├── input/
│   │   │   │   │   │                       └── output/
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
│   │       │   ├── bin/
│   │       │   │   ├── main/
│   │       │   │   │   └── com/
│   │       │   │   │       └── suchika/
│   │       │   │   │           ├── finance/
│   │       │   │   │           │   └── adapters/
│   │       │   │   │           └── wealth/
│   │       │   │   │               └── adapters/
│   │       │   │   │                   ├── http/
│   │       │   │   │                   │   └── dto/
│   │       │   │   │                   ├── persistence/
│   │       │   │   │                   └── services/
│   │       │   │   └── test/
│   │       │   │       └── com/
│   │       │   │           └── suchika/
│   │       │   │               ├── finance/
│   │       │   │               │   └── adapters/
│   │       │   │               └── wealth/
│   │       │   │                   └── adapters/
│   │       │   │                       └── services/
│   │       │   ├── src/
│   │       │   │   ├── main/
│   │       │   │   │   ├── java/
│   │       │   │   │   │   └── com/
│   │       │   │   │   │       └── suchika/
│   │       │   │   │   │           ├── finance/
│   │       │   │   │   │           │   └── adapters/
│   │       │   │   │   │           └── wealth/
│   │       │   │   │   │               └── adapters/
│   │       │   │   │   │                   ├── http/
│   │       │   │   │   │                   │   └── dto/
│   │       │   │   │   │                   ├── persistence/
│   │       │   │   │   │                   └── services/
│   │       │   │   │   └── resources/
│   │       │   │   │       ├── db/
│   │       │   │   │       │   └── migration/
│   │       │   │   │       └── application.properties
│   │       │   │   └── test/
│   │       │   │       └── java/
│   │       │   │           └── com/
│   │       │   │               └── suchika/
│   │       │   │                   ├── finance/
│   │       │   │                   │   └── adapters/
│   │       │   │                   └── wealth/
│   │       │   │                       └── adapters/
│   │       │   │                           └── services/
│   │       │   └── build.gradle.kts
│   │       ├── bin/
│   │       │   ├── main/
│   │       │   │   └── com/
│   │       │   │       └── suchika/
│   │       │   │           └── finance/
│   │       │   └── test/
│   │       │       └── com/
│   │       │           └── suchika/
│   │       │               └── finance/
│   │       ├── domain/
│   │       │   ├── bin/
│   │       │   │   ├── main/
│   │       │   │   │   └── com/
│   │       │   │   │       └── suchika/
│   │       │   │   │           ├── finance/
│   │       │   │   │           │   └── domain/
│   │       │   │   │           └── wealth/
│   │       │   │   │               └── domain/
│   │       │   │   └── test/
│   │       │   │       └── com/
│   │       │   │           └── suchika/
│   │       │   │               └── finance/
│   │       │   │                   └── domain/
│   │       │   ├── src/
│   │       │   │   ├── main/
│   │       │   │   │   ├── java/
│   │       │   │   │   │   └── com/
│   │       │   │   │   │       └── suchika/
│   │       │   │   │   │           ├── finance/
│   │       │   │   │   │           │   └── domain/
│   │       │   │   │   │           └── wealth/
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
│   │       │   ├── bin/
│   │       │   │   ├── main/
│   │       │   │   │   └── com/
│   │       │   │   │       └── suchika/
│   │       │   │   │           ├── finance/
│   │       │   │   │           │   └── ports/
│   │       │   │   │           └── wealth/
│   │       │   │   │               └── ports/
│   │       │   │   │                   ├── input/
│   │       │   │   │                   └── output/
│   │       │   │   └── test/
│   │       │   │       └── com/
│   │       │   │           └── suchika/
│   │       │   │               └── finance/
│   │       │   │                   └── ports/
│   │       │   ├── src/
│   │       │   │   ├── main/
│   │       │   │   │   ├── java/
│   │       │   │   │   │   └── com/
│   │       │   │   │   │       └── suchika/
│   │       │   │   │   │           ├── finance/
│   │       │   │   │   │           │   └── ports/
│   │       │   │   │   │           └── wealth/
│   │       │   │   │   │               └── ports/
│   │       │   │   │   │                   ├── input/
│   │       │   │   │   │                   └── output/
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
│   ├── finance/
│   ├── flyway/
│   │   ├── health/
│   │   │   ├── V1__init_health.sql
│   │   │   └── V2__remove_enum_constraints.sql
│   │   ├── household/
│   │   │   ├── V1__init_household.sql
│   │   │   ├── V2__goals.sql
│   │   │   └── V3__remove_enum_constraints.sql
│   │   ├── profile/
│   │   │   ├── V1__init_profile.sql
│   │   │   └── V2__add_admin_table.sql
│   │   ├── projections/
│   │   │   └── V1__init_projections.sql
│   │   ├── test-seed/
│   │   │   ├── health/
│   │   │   │   └── R__seed_health_test_data.sql
│   │   │   ├── profile/
│   │   │   │   └── R__seed_profile_test_data.sql
│   │   │   └── wealth/
│   │   │       └── R__seed_wealth_test_data.sql
│   │   ├── wealth/
│   │   │   ├── V1__init_ledger.sql
│   │   │   ├── V2__physical_assets.sql
│   │   │   ├── V3__upload_status.sql
│   │   │   ├── V4__enrich_account.sql
│   │   │   └── V5__remove_enum_constraints.sql
│   │   └── 00_bootstrap.sql
│   └── web-gateway/
│       ├── bin/
│       │   ├── main/
│       │   │   └── com/
│       │   │       └── suchika/
│       │   │           └── gateway/
│       │   │               ├── health/
│       │   │               ├── profile/
│       │   │               └── wealth/
│       │   └── test/
│       │       └── com/
│       │           └── suchika/
│       │               └── gateway/
│       │                   ├── health/
│       │                   ├── profile/
│       │                   └── wealth/
│       ├── src/
│       │   ├── main/
│       │   │   ├── java/
│       │   │   │   └── com/
│       │   │   │       └── suchika/
│       │   │   │           └── gateway/
│       │   │   │               ├── health/
│       │   │   │               ├── profile/
│       │   │   │               └── wealth/
│       │   │   └── resources/
│       │   │       ├── application.properties
│       │   │       ├── health.yaml
│       │   │       ├── household.yaml
│       │   │       ├── profile.yaml
│       │   │       ├── shared.yaml
│       │   │       └── wealth.yaml
│       │   └── test/
│       │       └── java/
│       │           └── com/
│       │               └── suchika/
│       │                   └── gateway/
│       │                       ├── health/
│       │                       ├── profile/
│       │                       └── wealth/
│       └── build.gradle.kts
├── assets/
│   └── images/
│       ├── darkmode.png
│       ├── darkmode.psd
│       ├── github-logo.png
│       ├── hackerrank_logo.png
│       ├── lightmode.png
│       ├── linkedin-logo.png
│       ├── logo.svg
│       ├── logo192.png
│       ├── logo512.png
│       ├── map.png
│       └── WhatsApp-logo.png
├── documents/
│   ├── ToBeDeleted/
│   ├── AGENTS.md
│   ├── ARCHITECTURE_DECISIONS.md
│   ├── ARCHITECTURE_GUIDELINES.md
│   ├── ARCHITECTURE_PROPOSALS.md
│   ├── BUSINESS_REQUIREMENTS.md
│   ├── CICD.md
│   ├── FRONTEND_GUIDELINES.md
│   ├── LOGGING_AND_EXCEPTIONS.md
│   ├── QA_API_TEST_RESULTS.md
│   ├── REQUIREMENTS_cross_domain.md
│   ├── REQUIREMENTS_health_domain.md
│   ├── REQUIREMENTS_household_domain.md
│   ├── REQUIREMENTS_wealth_domain.md
│   ├── ROADMAP.md
│   └── V02_DEVELOPMENT_PLAN.md
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── infrastructure/
│   ├── local/
│   ├── resources/
│   └── build.gradle.kts
├── scripts/
│   ├── build-local.ps1
│   ├── build-local.sh
│   ├── build-service.ps1
│   ├── check-migrations-location.sh
│   ├── check-prerequisites.ps1
│   ├── check-prerequisites.sh
│   ├── clean-all.ps1
│   ├── clean-builds.ps1
│   ├── db-reset.ps1
│   ├── db-shell.ps1
│   ├── db-start.ps1
│   ├── dev-aliases.ps1
│   ├── dev-service.ps1
│   ├── documentWriter.py
│   ├── generate-api.ps1
│   ├── health-check.ps1
│   ├── health-check.sh
│   ├── logs.ps1
│   ├── setup-dev.ps1
│   ├── sonar-scan.ps1
│   ├── sonar-start.ps1
│   ├── stop-all.ps1
│   └── test-service.ps1
├── shared/
│   ├── bin/
│   │   ├── main/
│   │   │   └── com/
│   │   │       └── suchika/
│   │   │           └── shared/
│   │   │               ├── dto/
│   │   │               ├── exception/
│   │   │               ├── logging/
│   │   │               └── mapper/
│   │   └── test/
│   │       └── com/
│   │           └── suchika/
│   │               └── architecture/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   │   └── com/
│   │   │   │       └── suchika/
│   │   │   │           └── shared/
│   │   │   │               ├── dto/
│   │   │   │               ├── exception/
│   │   │   │               ├── logging/
│   │   │   │               └── mapper/
│   │   │   └── resources/
│   │   │       └── META-INF/
│   │   │           └── beans.xml
│   │   └── test/
│   │       └── java/
│   │           └── com/
│   │               └── suchika/
│   │                   └── architecture/
│   └── build.gradle.kts
├── web/
│   ├── public/
│   │   ├── favicon.ico
│   │   ├── index.html
│   │   ├── manifest.json
│   │   └── robots.txt
│   ├── src/
│   │   ├── api/
│   │   │   ├── generated.d.ts
│   │   │   └── generated.ts
│   │   ├── components/
│   │   │   └── shared/
│   │   │       └── ComingSoon.jsx
│   │   ├── context/
│   │   ├── hooks/
│   │   ├── pages/
│   │   │   ├── Admin/
│   │   │   ├── Health/
│   │   │   ├── Household/
│   │   │   ├── Public/
│   │   │   ├── User/
│   │   │   └── Wealth/
│   │   ├── types/
│   │   │   ├── auth.ts
│   │   │   ├── health.ts
│   │   │   ├── household.ts
│   │   │   ├── index.ts
│   │   │   └── wealth.ts
│   │   ├── utils/
│   │   └── index.css
│   ├── jsconfig.json
│   ├── package-lock.json
│   └── package.json
├── build.gradle.kts
├── CLAUDE.md
├── CODE_OF_CONDUCT.md
├── CONTRIBUTING.md
├── gradle.properties
├── gradlew
├── gradlew.bat
├── LICENSE
├── package-lock.json
├── README.md
├── SECURITY.md
├── settings.gradle.kts
└── sonar-project.properties
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