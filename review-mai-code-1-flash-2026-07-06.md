AI Model Used:
MAI-Code-1-Flash

Model Version (if known):
Not specified

Review Date:
2026-07-06

Review Time (start–end):
23:47–23:58

## Architecture
Summary:
The repository is structurally aligned with a hexagonal, multi-service layout and the web layer is correctly separated behind the gateway. The main weaknesses are duplicated pagination contracts, weak boundary validation, and a few places where contracts are still more implicit than contract-first.

Findings:
1. Pagination DTOs are duplicated per domain instead of being shared — Severity: Medium — File(s): [application/domain/wealth/ports/src/main/java/com/suchika/wealth/ports/input/PagedTransactions.java](application/domain/wealth/ports/src/main/java/com/suchika/wealth/ports/input/PagedTransactions.java), [application/domain/health/ports/src/main/java/com/suchika/health/ports/input/PagedDoctorVisits.java](application/domain/health/ports/src/main/java/com/suchika/health/ports/input/PagedDoctorVisits.java), [application/domain/health/ports/src/main/java/com/suchika/health/ports/input/PagedVitalReadings.java](application/domain/health/ports/src/main/java/com/suchika/health/ports/input/PagedVitalReadings.java) — Recommendation: Introduce one shared pagination record or generic response type in the shared module and replace the three domain-specific variants so list APIs converge on a single contract.
2. Boundary validation is effectively absent in the Java command records — Severity: High — File(s): [application/domain/wealth/ports/src/main/java/com/suchika/wealth/ports/input/CreateAccountCommand.java](application/domain/wealth/ports/src/main/java/com/suchika/wealth/ports/input/CreateAccountCommand.java), [application/domain/health/ports/src/main/java/com/suchika/health/ports/input/CreateDoctorVisitCommand.java](application/domain/health/ports/src/main/java/com/suchika/health/ports/input/CreateDoctorVisitCommand.java), [application/domain/wealth/ports/src/main/java/com/suchika/wealth/ports/input/CreateTransactionCommand.java](application/domain/wealth/ports/src/main/java/com/suchika/wealth/ports/input/CreateTransactionCommand.java) — Recommendation: Add Jakarta validation annotations such as @NotNull, @NotBlank, @Positive, and @Size to the input records and wire the adapters to enforce them at the HTTP boundary.
3. The OpenAPI contracts are not consistently enforcing the same level of validation as the Java ports layer — Severity: Medium — File(s): [application/contract/health.yaml](application/contract/health.yaml), [application/contract/wealth.yaml](application/contract/wealth.yaml), [application/contract/shared.yaml](application/contract/shared.yaml) — Recommendation: Tighten the schema rules for minor fields as well as primary fields (for example, minimum/maximum lengths, required fields, non-negative amounts) so the contract is the authoritative first line of validation.

## Business logic (V1 scope only)
Summary:
The core domain logic is largely pure and framework-free, which is a strength for V1. The main concern is that one calculator still reaches out to the system clock internally, making it less deterministic and less testable than it should be.

Findings:
1. The amortization calculator depends on the system clock inside the domain method — Severity: Medium — File(s): [application/domain/wealth/domain/src/main/java/com/suchika/wealth/domain/AmortizationCalculator.java](application/domain/wealth/domain/src/main/java/com/suchika/wealth/domain/AmortizationCalculator.java) — Recommendation: Change the method to accept an explicit as-of date parameter so the calculation is deterministic and easier to unit test without relying on the server clock.

## DevOps
Summary:
The repo has good developer-facing scripts and a Linux/Codespaces shell entrypoint, but the bootstrap story is still split across Windows PowerShell and Linux-specific paths. That makes onboarding and reproducibility less simple than the rest of the architecture.

Findings:
1. The bootstrap path is fragmented between Windows and Codespaces flows — Severity: Medium — File(s): [scripts/setup-dev.ps1](scripts/setup-dev.ps1), [scripts/dev-aliases.sh](scripts/dev-aliases.sh), [.devcontainer/setup.sh](.devcontainer/setup.sh) — Recommendation: Add a single cross-platform bootstrap entrypoint and a smoke-test script that verifies database readiness, service startup, and frontend availability without relying on separate manual paths.
2. The repo relies on a large set of alias-based helpers rather than one explicit startup contract — Severity: Low — File(s): [scripts/dev-aliases.ps1](scripts/dev-aliases.ps1), [scripts/dev-aliases.sh](scripts/dev-aliases.sh), [documents/SCRIPTS.md](documents/SCRIPTS.md) — Recommendation: Document one canonical startup sequence and make the scripts enforce it consistently so contributors do not need to infer the dependency order from multiple files.

## Health domain
Summary:
The health domain has a clear vertical slice for vitals and doctor visits, but the request contracts are still weakly validated and the boundary does not enforce basic correctness before the use cases receive the payload.

Findings:
1. Doctor-visit commands have no validation annotations at the ports boundary — Severity: High — File(s): [application/domain/health/ports/src/main/java/com/suchika/health/ports/input/CreateDoctorVisitCommand.java](application/domain/health/ports/src/main/java/com/suchika/health/ports/input/CreateDoctorVisitCommand.java), [application/domain/health/ports/src/main/java/com/suchika/health/ports/input/UpdateDoctorVisitCommand.java](application/domain/health/ports/src/main/java/com/suchika/health/ports/input/UpdateDoctorVisitCommand.java) — Recommendation: Add validation annotations for required date fields, non-blank identifiers, and constrained strings, and make the adapters reject malformed payloads before they hit the domain layer.
2. The health OpenAPI request schemas are permissive for minor fields — Severity: Medium — File(s): [application/contract/health.yaml](application/contract/health.yaml) — Recommendation: Add explicit minLength/maxLength and required-field rules for fields such as doctor_name, hospital_name, speciality, and notes so the contract reflects the intended data quality rules.

## Wealth domain
Summary:
The wealth domain is functionally rich and includes several advanced capabilities, but it also carries more implementation complexity than the current feature set seems to justify. The persistence layer especially mixes query-building concerns in a way that is harder to read than necessary.

Findings:
1. The wealth migration history is still split across multiple Flyway files instead of a single baseline script — Severity: Medium — File(s): [application/flyway/wealth/V1__init_wealth_consolidated.sql](application/flyway/wealth/V1__init_wealth_consolidated.sql), [application/flyway/wealth/V2__transaction_dedup_key_fix.sql](application/flyway/wealth/V2__transaction_dedup_key_fix.sql) — Recommendation: Merge the follow-up change into the consolidated baseline and remove the extra version so local resets and migration review remain simple.
2. Transaction query filtering is implemented with hand-built JPQL string assembly — Severity: Medium — File(s): [application/domain/wealth/adapters/src/main/java/com/suchika/wealth/adapters/persistence/TransactionPanacheRepository.java](application/domain/wealth/adapters/src/main/java/com/suchika/wealth/adapters/persistence/TransactionPanacheRepository.java) — Recommendation: Replace the custom filter builder with simpler, more explicit repository methods or a small helper that preserves the profile-scoped logic but reduces the amount of string assembly and parameter bookkeeping.

## Household domain
Summary:
The household domain is feature-complete for the current scope and follows the same modular pattern as the other domains. The clearest issue is that its migration history is still not consolidated into one baseline script.

Findings:
1. Household still has a second Flyway migration file instead of a single consolidated baseline — Severity: Low — File(s): [application/flyway/household/V1__init_household_consolidated.sql](application/flyway/household/V1__init_household_consolidated.sql), [application/flyway/household/V2__restore_not_null_constraints.sql](application/flyway/household/V2__restore_not_null_constraints.sql) — Recommendation: Fold the constraint restoration change into the primary consolidated migration and delete the follow-up file to keep the database evolution path straightforward.

## Profile domain
Summary:
The profile domain is consistent with the repository’s architecture and serves as the identity anchor well. The main issue is the same migration churn seen elsewhere, which adds avoidable maintenance overhead.

Findings:
1. Profile still ships a second migration file rather than a single consolidated baseline — Severity: Low — File(s): [application/flyway/profile/V1__init_profile_consolidated.sql](application/flyway/profile/V1__init_profile_consolidated.sql), [application/flyway/profile/V2__drop_unused_profile_metadata.sql](application/flyway/profile/V2__drop_unused_profile_metadata.sql) — Recommendation: Merge the metadata cleanup into the consolidated V1 migration and remove the extra version so schema history stays easier to reason about.

## Quality/testing
Summary:
The repository has a strong layer of unit and integration tests, and the frontend has extensive Jest and Playwright coverage. The missing piece is backend contract testing, which leaves API drift between the OpenAPI contracts and the running services less guarded than the rest of the stack.

Findings:
1. Contract tests are not present even though the repo already maintains OpenAPI contracts and generated clients — Severity: High — File(s): [application/contract/gateway.yaml](application/contract/gateway.yaml), [application/contract/health.yaml](application/contract/health.yaml), [application/contract/wealth.yaml](application/contract/wealth.yaml), [web/src/api/generated.ts](web/src/api/generated.ts) — Recommendation: Add automated contract-validation tests that exercise the running HTTP layer and fail when the implementation drifts from the published schema.
2. Frontend coverage is broad, but it does not fully compensate for missing backend contract enforcement — Severity: Medium — File(s): [web/e2e](web/e2e), [web/src](web/src) — Recommendation: Prioritize backend contract tests and schema-driven validation over additional frontend-only polish so correctness is guarded at the API boundary first.

## Documentation
Summary:
The documentation set is extensive, but some of the milestone status documents are now inconsistent with the current project state. That creates avoidable confusion for anyone onboarding from the docs rather than the source.

Findings:
1. The milestone status in the business requirements document is out of sync with the current project snapshot — Severity: Medium — File(s): [documents/BUSINESS_REQUIREMENTS.md](documents/BUSINESS_REQUIREMENTS.md), [documents/CONTEXT_PRIMER.md](documents/CONTEXT_PRIMER.md), [documents/ROADMAP.md](documents/ROADMAP.md) — Recommendation: Update the business requirements document so it reflects the current v0.6 status, the completed household work, and the intended next milestone instead of describing older milestone expectations.

Overall Priority List
1. Add contract-first validation at the Java ports boundary and in the OpenAPI contracts — Severity: High.
2. Add automated backend contract tests so API shape drift is caught before it reaches the frontend — Severity: High.
3. Replace the duplicated pagination DTOs with one shared contract — Severity: Medium.
4. Consolidate the follow-up Flyway scripts into a single baseline per domain — Severity: Medium.
5. Remove the system-clock dependency from the amortization calculator so business logic is deterministic — Severity: Medium.
