AI Model Used: Gemini
Model Version (if known): 3.1 Pro (High)
Review Date: 2026-07-06
Review Time (start–end): 21:41–21:55

## Deep Architecture Overview
- **Backend (Java 17 / Quarkus 3.29.0)**: Strictly implements **Hexagonal Architecture (Ports and Adapters)** across bounded contexts (`household`, `profile`, `wealth`, `contract`, `finance`). Domain logic is pure (e.g., `InventoryItem` using immutable Builders). Adapters handle framework logic (`@ApplicationScoped`) and persistence via **Hibernate ORM with Panache**.
- **Frontend (React / Tailwind CSS)**: A modern **React SPA** utilizing functional components, hooks, and **Tailwind CSS** for UI utilities. It includes comprehensive testing via Jest/React Testing Library and **Playwright** for E2E tests.

## Architecture
Summary: The project strictly adheres to Hexagonal Architecture, successfully isolating pure domain logic from frameworks, and using a gateway for aggregation. However, contract validation is missing and pagination patterns are duplicated.
Findings:
1. Duplicated Pagination Contracts — Severity: Medium — File(s): `application/domain/wealth/ports/src/main/java/com/suchika/wealth/ports/input/PagedTransactions.java`, `application/domain/health/ports/src/main/java/com/suchika/health/ports/input/PagedDoctorVisits.java`, `application/domain/health/ports/src/main/java/com/suchika/health/ports/input/PagedVitalReadings.java` — Recommendation: Extract these duplicated DTO structures into a single, generic `Page<T>` or `Pagination<T>` record within the `shared` module.
2. Missing Validation on Contracts — Severity: High — File(s): `application/domain/wealth/ports/src/main/java/com/suchika/wealth/ports/input/CreateAccountCommand.java`, `application/domain/health/ports/src/main/java/com/suchika/health/ports/input/CreateDoctorVisitCommand.java`, and other commands in `ports/input` — Recommendation: Implement contract-first validation by adding standard `jakarta.validation` annotations (like `@NotNull`, `@NotBlank`) to enforce constraints on all minor and major fields at the boundary.

## Business logic (V1 scope only)
Summary: Core business logic relies on pure Java domain models avoiding framework dependencies, but contains some testability flaws and potential over-engineering.
Findings:
1. System Clock Coupling — Severity: Medium — File(s): `application/domain/wealth/domain/src/main/java/com/suchika/wealth/domain/AmortizationCalculator.java` — Recommendation: Pass `LocalDate today` as a parameter to the `compute()` method instead of calling `LocalDate.now(ZoneId.of("Asia/Kolkata"))` internally. This removes system clock dependency and allows deterministic unit testing.

## DevOps
Summary: The project uses Gradle and provides scripts for local setup, but Linux/Codespaces support relies heavily on aliases rather than full script parity with Windows.
Findings:
1. Incomplete Bash Script Parity for Codespaces — Severity: Medium — File(s): `scripts/setup-dev.ps1`, `scripts/db-start.ps1`, `scripts/clean-all.ps1` — Recommendation: Create `.sh` equivalents for all critical `.ps1` scripts to ensure the repository boots natively and cleanly in Codespaces/Linux environments without relying solely on `dev-aliases.sh`.

## Health domain
Summary: Implements vital readings and doctor visits. Needs robust contract validation on commands.
Findings:
1. Missing Validation in Contracts — Severity: High — File(s): `application/domain/health/ports/src/main/java/com/suchika/health/ports/input/CreateDoctorVisitCommand.java`, `UpdateDoctorVisitCommand.java` — Recommendation: Add standard `@NotNull` annotations to enforce required fields such as `profileId`, `fromDate`, and `toDate`.

## Wealth domain
Summary: Manages accounts, transactions, and physical assets, including complex deduplication and loan calculations. Contains unmerged migrations.
Findings:
1. Unmerged Flyway Scripts — Severity: Low — File(s): `application/flyway/wealth/V1__init_wealth_consolidated.sql`, `application/flyway/wealth/V2__transaction_dedup_key_fix.sql` — Recommendation: Merge V2 into the consolidated V1 script. Since pre-v1.0 data is ephemeral, you can drop the DB schema, combine the SQL, and re-run Flyway for a cleaner baseline.

## Household domain
Summary: Manages calendar events, goals, and inventory. Also contains unmerged migrations.
Findings:
1. Unmerged Flyway Scripts — Severity: Low — File(s): `application/flyway/household/V1__init_household_consolidated.sql`, `application/flyway/household/V2__restore_not_null_constraints.sql` — Recommendation: Merge the V2 constraint changes into the V1 consolidated script.

## Profile domain
Summary: Anchors identity for the system. Also contains unmerged migrations.
Findings:
1. Unmerged Flyway Scripts — Severity: Low — File(s): `application/flyway/profile/V1__init_profile_consolidated.sql`, `application/flyway/profile/V2__drop_unused_profile_metadata.sql` — Recommendation: Remove the unused metadata column directly in the V1 script and delete V2.

## Quality/testing
Summary: Good backend test coverage and ArchUnit enforcement. Playwright web tests cover major UI flows, but missing backend contract tests remain a gap.
Findings:
1. Missing Contract Tests — Severity: High — File(s): Repository-wide — Recommendation: Prioritizing backend correctness, implement OpenAPI contract validation tests (as noted in ROADMAP.md) to ensure endpoints strictly fulfill the defined YAML contracts.

## Documentation
Summary: Comprehensive documentation exists but is heavily fragmented and contains contradictory version information between key primer documents.
Findings:
1. Contradictory Milestone Status — Severity: Medium — File(s): `documents/BUSINESS_REQUIREMENTS.md` vs `documents/CONTEXT_PRIMER.md` — Recommendation: Update `BUSINESS_REQUIREMENTS.md` (which claims v0.2 UAT and Household deferred) to align with `CONTEXT_PRIMER.md` and `ROADMAP.md` (which accurately claim v0.6 is complete and Household is delivered) to prevent onboarding confusion.

## Overall Priority List
1. Missing Validation on Contracts (Architecture / Health) — Severity: High — Without contract-first validation, the backend could ingest malformed data, undermining data hygiene for future AI usage.
2. Missing Contract Tests (Quality/testing) — Severity: High — Enforcing API contracts is crucial to ensure backend correctness over frontend polish.
3. Duplicated Pagination Contracts (Architecture) — Severity: Medium — Extracting `PagedTransactions`, `PagedDoctorVisits`, and `PagedVitalReadings` into a shared contract immediately reduces boilerplate and aligns with shared contract principles.
4. Contradictory Milestone Status (Documentation) — Severity: Medium — Misaligned documents (`BUSINESS_REQUIREMENTS.md` vs `CONTEXT_PRIMER.md`) create confusion and slow down agent/developer onboarding.
5. System Clock Coupling (Business logic) — Severity: Medium — Passing `LocalDate` directly into `AmortizationCalculator` instead of instantiating it internally will improve the purity and testability of the business logic.
