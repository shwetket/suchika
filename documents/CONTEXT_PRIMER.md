# Context Primer — Suchika

| | |
|---|---|
| **Type** | Reference |
| **Audience** | AI agents, new developers |
| **Status** | Active |
| **Last updated** | 2026-07-13 (ADR-023 added — Application Console design) |

## Objective

Provide a compact project snapshot that any agent or developer can read in ~2 minutes to get full context before starting work. This is the mandatory first read — everything else in `documents/` is depth on a specific topic.

## Use Cases

- **Always** — read this before starting any task in this repo
- After a long gap away from the project — re-sync on current version and domain status
- When handing off work to another agent or developer

---

**Read this first.** Compact project snapshot for fast agent bootstrapping. ~2 min read.
For depth, follow the links to domain-state files or documents/.

---

## What Is Suchika

Personal household management system. Four domains: Profile, Wealth, Health, Household.
Hexagonal Architecture (Ports & Adapters). Single PostgreSQL database (`app_db`). React frontend.

---

## Current Version: v0.6 — Complete (2026-07-03)

| Domain | Backend | Frontend | Status |
|---|---|---|---|
| Profile | ✅ | ✅ | Complete — policy settings (Epic 8 Ph4); v0.6 adapter/domain tests added |
| Wealth | ✅ | ✅ | Complete — full Epic 8 engine + v0.5 fixes + v0.6 transaction pagination |
| Health | ✅ | ✅ | Complete — v0.5 vitals edit + v0.6 doctor visit date-range filter |
| Household | ✅ | ✅ | Complete — v0.3 + v0.5 inventory edit/is_consumed + v0.6 Goals copy note |
| web-gateway | ✅ | — | 12 CQRS projection steps live (Action Center added in v0.5) |

**Next milestone: v0.7** — not yet planned; see `ROADMAP.md` for the full future-milestone list (v1.0 Security & Persistence is next after any v0.7 gap-filling).

**Pre-v1.0 retrospective — 2026-07-06:** full-repo review (architect, business-analyst, all domain/infra/frontend agents); no version bump. See `ROADMAP.md`'s "Architect Review — 2026-07-06" and "Business Analyst Review — 2026-07-06" sections; all four `domain-state/*.md` files were refreshed the same day.

**Post-retrospective UX/data pass — 2026-07-08 through 2026-07-11 (no version bump, still v0.6):**
- v0.5.1 Workstream (2026-07-08/09): `profile_id`/`admin_id` enforcement (Tier A all domains, Tier B wealth-only pilot), Flyway V2→V1 merges (profile/wealth/household), ADR-021 (login auto-attaches to the sole active admin instead of a broken `localStorage` carry-forward).
- Real seeded household data loaded (2026-07-10) — `application/flyway/test-seed/{health,household,profile,wealth}/R__seed_*_test_data.sql`, 4 real profiles + 46 real wealth accounts + 8 real physical assets. **Flag:** each seed file's own header comment claims it is "gitignored... on the seed-data branch, never pushed/merged," but on this branch (`UX-Updates`) `.gitignore` has no `test-seed` entry and all four files are tracked/committed — real names, a real email address, real bank/account details, and real financial figures are currently in git history on this branch. Worth a deliberate decision (gitignore + untrack, or replace with synthetic data) before this branch merges — see `documents/domain-state/profile.md` Open Issues for the same flag.
- `AuthContext.login()` now filters out inactive admins before the single-vs-multiple-admin check (2026-07-11) — closes a real bug where one active admin alongside any deactivated admin was wrongly treated as a multi-household conflict.
- Shared `Badge` (`web/src/components/shared/Badge.js`) and `EditIcon` (`web/src/components/shared/EditIcon.js`) components introduced and adopted across Profiles, Accounts, Dashboard, PhysicalAssets, Reports, and several Public/Admin pages — icon-only edit actions, deactivate-moved-into-edit-modal, and reactivate-from-edit-modal patterns are now consistent across Profile and Wealth pages. New CSS design tokens/`card-hover` utility added in `web/src/index.css` + `tailwind.config.js`.
- Wealth UX pass (UX-001 through UX-018, `documents/UX_DECISIONS.md`) plus new `wealth.account.balance_as_of` field (Flyway V3) — see `documents/domain-state/wealth.md` for full detail, already current as of 2026-07-11.

Quality gates (v0.6): all Gradle tests green (512+ backend tests total), ArchUnit clean including a new port-interface test-coverage rule, Jest branch coverage gate enforced (branches 70%, lines 80%). 539+ JS tests as of the 2026-07-11 wealth UX pass, 30 gateway projection tests.

**Platform improvements — 2026-07-13 (no version bump, still v0.6):**
- **Java 17 → 25 upgrade (Phase 0 of the platform-improvements plan):** `build.gradle.kts` `sourceCompatibility`/`targetCompatibility` bumped `VERSION_17` → `VERSION_25`, plus a Gradle toolchain (`languageVersion = JavaLanguageVersion.of(25)`) and the `org.gradle.toolchains.foojay-resolver-convention` plugin in `settings.gradle.kts` so a matching JDK auto-provisions on machines that don't have one. Full repo-wide `./gradlew test` (all 4 domains + gateway + shared/ArchUnit, 512+ tests) verified clean on JDK 25 bytecode with zero regressions — no Mockito/ByteBuddy/JaCoCo issues. `.devcontainer/docker-compose.yml` base image bumped `java:17` → `java:25` (confirmed the tag exists), `ci.yml`'s `setup-java` bumped `21` → `25`, `check-prerequisites.ps1`/`.sh` thresholds now hard-fail below 21 (was: 17-20 only warned) and target 25. Closes the Java-version drift flagged in the 2026-07-06 retrospective.
- **Script & tooling foundation (Phase 1):** new `scripts/services.json` is now the single source of truth for ports, DB schema names, Gradle module/task wiring, the DB password fallback, and Java/Node version floors — previously duplicated independently across `dev-aliases.ps1`, `dev-aliases.sh`, `stop-all.ps1`, `health-check.ps1`/`.sh`, `dev-service.ps1`, `db-reset.ps1`, and `check-prerequisites.ps1`/`.sh`; all of those now read from it via new loaders `scripts/config.ps1` / `scripts/config.sh` (bash side parses with `grep -oP`, no `jq` dependency — see `documents/SCRIPTS.md`). New PID-file service registry (`scripts/service-registry.ps1`/`.sh`, files at `~/.suchika/run/<service>.pid`) tracks the real OS process behind each running service (not the GUI-terminal-window wrapper on Windows, nor the `gradlew` wrapper on bash) — `stop-all`/`sa` and `status` use it first, falling back to the old port-based detection only when no PID record exists; this is the primitive a future simplified local-run mode and admin console are expected to build on, not something built for its own sake. All 5 backend `build.gradle.kts` (`profile`/`wealth`/`health`/`household` adapters + `web-gateway`) now declare `quarkus-smallrye-health`, and `health-check.ps1`/`.sh` (`status`) hit the real `GET /q/health` instead of misusing `/q/openapi` — a service returning 5xx now correctly reports DOWN instead of UP. `scripts/documentWriter.py` (unmaintained, no dry-run gate, already suspected of corrupting `GETTING_STARTED.md`) was deleted outright — the `document-writer` subagent already covers its job. New `scripts/check-ps1-bom.sh` (mirrors `check-migrations-location.sh`'s pattern) enforces the existing UTF-8-BOM-on-`.ps1`-files convention via `.husky/pre-commit` and CI instead of leaving it as an unenforced doc note. `clean-all.ps1` and `setup-dev.ps1` confirmed to have no bash equivalent by design (Codespaces' disposable-container model and `.devcontainer/setup.sh`'s automatic `postCreateCommand` cover the same need respectively) and now say so explicitly in `documents/SCRIPTS.md` instead of leaving it implicit.
- **Backend logging standardization (Phase 2):** exactly 4 logging conventions project-wide — INFO/WARNING/ERROR/HEALTH, **no DEBUG level anywhere**. `AppLogger` (`shared/`) had `debug()` removed entirely (zero call sites existed; only the doc recommended it — that mismatch is fixed too) and gained `health(String, Object...)`, which logs at INFO severity through a **dedicated JBoss Logging category** `com.suchika.health` (`Logger.getLogger("com.suchika.health")`, a separate instance from the `io.quarkus.logging.Log`-backed rest of the class) rather than inventing a custom `java.util.logging.Level` — filterable via the `%c{3.}` category already in every log format. Not to be confused with the unrelated health *domain* (vitals/doctor-visits). `ApplicationExceptionMapper` fixed to log 4xx at `warn` / 5xx at `error` (previously logged everything at WARN, even 500s — confirmed and covered by a new `ApplicationExceptionMapperTest` that captures actual JUL log records via a `Handler` to assert the level split). Two new ArchUnit rules in `DomainRulesTest`: `application_code_must_not_use_raw_loggers` widened to also ban `org.jboss.logging..` (previously only slf4j/JUL), now scoped to a `nonTestClasses` import (`ImportOption.Predefined.DO_NOT_INCLUDE_TESTS`) so test-only log-capture tooling doesn't false-positive, and exempts only `AppLogger.java` itself (was: the whole `shared` package) — both new rules were sanity-checked to actually fail the build against a temporarily-reintroduced violation before being kept; plus `app_logger_must_not_declare_a_debug_method` / `application_code_must_not_call_debug_methods` lock the no-DEBUG rule in at compile time. All 5 `application.properties` (profile/wealth/health/household/web-gateway): `%dev.quarkus.log.file.level` dropped `DEBUG` → `INFO`; added `quarkus.log.console.format` (same pattern as the existing file format, applied unscoped so it's active in all profiles, not just `%dev`) since none previously set an explicit console format. `documents/LOGGING_AND_EXCEPTIONS.md` rewritten to match all of the above.
- **Simplified local run (Phase 3):** new `run-local`/`rl` and `stop-local`/`stopl` aliases give a headless "just start the app" mode alongside the existing `da`/`sa`, which are untouched — still visible windows, still the right choice for active development. New `scripts/run-local.ps1` starts all 5 backend services + frontend with **zero GUI windows on Windows** (`Start-Process -WindowStyle Hidden`, a real change from `dev-service.ps1`'s always-opens-a-window behavior), reusing Phase 1's `Register-SuchikaServiceAsync` for PID tracking (not reimplemented) and a new `Wait-SuchikaHealthy` helper (added to `config.ps1`/`config.sh`) to poll real `/q/health` in the mandatory profile → wealth/health/household → gateway → frontend order before declaring the stack ready. `scripts/run-local.sh` is a thin wrapper around the pre-existing `dev-all` — bash's dev mode was already headless (no GUI-window concept in a Codespaces/Linux terminal to bring to parity with), so the only reason `run-local.sh` exists is so the command reads the same on both platforms. `stop-local.ps1`/`.sh` are both thin wrappers around `stop-all` for the identical reason in reverse: Phase 1's PID-first-then-port-fallback kill logic already handles a headlessly-started service exactly the same as a GUI-started one, since both register into the same `~/.suchika/run/<service>.pid` files.
  **Three real bugs found and fixed during end-to-end verification** (the implementing agent got stuck mid-test and was restarted, then a from-scratch verification pass caught these): (1) `sl` was never a usable alias — it collides with PowerShell's own built-in `AllScope` `Set-Location` alias, which `Set-Alias -Force` cannot override; renamed to `stopl` on both PowerShell and bash for consistency. (2) `run-local.ps1` redirected gradle's stdout/stderr into the exact same file path (`<service>.log`) that each backend's own `%dev.quarkus.log.file.path` independently writes to — two OS handles on one file caused a reproduced Windows `LogManager`/`SizeRotatingFileHandler` open-or-rotate failure at startup; fixed by routing gradle's console output to a separate `<service>.console.log` sidecar and leaving `<service>.log` as Quarkus's sole structured-log writer (bash's `_dev_svc` has the same pattern but doesn't fail, since POSIX allows renaming a file out from under an open handle — left as-is, out of this phase's scope). (3) The actual blocker: `Get-SuchikaHealthUrl`/`suchika_wait_healthy` built health-check URLs against `localhost`, which resolves to `::1` before `127.0.0.1` — since every backend binds IPv4-only, the IPv6 attempt hung instead of failing fast in the tested environment, so `Wait-SuchikaHealthy` always reported services unhealthy even when `/q/health` was actually returning 200; fixed by using `127.0.0.1` directly in both `config.ps1` and `config.sh` (this also silently fixes the same latent bug in `da`'s own health wait, not just `run-local`). Full 6-service start/stop cycle re-verified clean after all three fixes: `status` showed all 7 (DB + 5 backend + frontend) UP with correct PIDs, and `stop-local` cleanly killed all 6 real processes via the registry.
- **Application Console — backend + frontend implemented (Phase 4), see ADR-023:** admin-only live service status, start/stop controls, and a per-domain error feed. Two architectural surfaces, both flag-gated (`suchika.console.enabled=false` by default in `application.properties` — every `ConsoleResource` endpoint 404s while off, verified by a dedicated `ConsoleResourceDisabledTest` that deliberately does not override the flag; `ConsoleResourceTest` covers the enabled behavior via a `@TestProfile` config override).
  - **(1) `com.suchika.gateway.console` package in `web-gateway`.** `ServiceControlService` shells out to `scripts/run-local.ps1`/`.sh` and `stop-local.ps1`/`.sh` using the new per-service `-Service <name>` argument added in this same phase (Part A prerequisite — extends, not duplicates, Phase 1/3's script machinery); OS-detected via `os.name` (the first Java-side cross-platform branch in this repo — every prior cross-platform decision here shipped as a separate `.ps1`/`.sh` file instead); resolves the repo root by walking up from the JVM's cwd looking for `scripts/services.json` (robust to Gradle's `quarkusDev` cwd differing from the repo root, with a `suchika.repo-root` override escape hatch). `ServiceStatusService` polls each service's real `/q/health` (127.0.0.1, not `localhost` — same IPv6-hang fix Phase 3 already applied) and reads the `~/.suchika/run/<service>.pid` registry files Phase 1 already writes. This remains a deliberate, scoped exception to the gateway's side-effect-free role (ADR-002/ADR-013), same rationale ADR-021 used for auto-attach-to-sole-admin — flagged for mandatory re-examination once real OIDC auth lands at v1.0 (ADR-005).
  - **(2) A new `error_log` table per domain** (own schema, own Flyway migration — `V2__error_log.sql` for profile/health/household, `V6__error_log.sql` for wealth since it was already at V5 — no shared cross-domain table, per ADR-003/ADR-006), each exposing `GET /v1/errors?since=&limit=` (mirrors `GET /uploads/{id}/errors`'s shape; deliberately separate from wealth's existing CSV-upload-specific `upload_error_log`). Populated via a new `com.suchika.shared.exception.ErrorLogRecorder` CDI port: `ApplicationExceptionMapper` (shared/, Phase 2's 4xx-WARN/5xx-ERROR logging unchanged) persists every `ApplicationException` after logging it, using `Instance<ErrorLogRecorder>` so the injection point stays satisfiable where no domain registers a bean (web-gateway has no DB). Each domain's `ErrorLogService` (adapters) implements both the shared write-side port and a domain-owned `ErrorLogUseCase` (read side) against the same table — full hexagonal vertical slice per domain (`ErrorLog` domain object, `ErrorLogRepository`/`ErrorLogUseCase` ports, `ErrorLogEntity`/`ErrorLogDao`/`ErrorLogPanacheRepository`/`ErrorLogService`/`ErrorLogResource` adapters). `web-gateway` adds `GET /v1/console/errors` (`ConsoleErrorAggregationService`) fanning out to all four domains via one new `listErrors` method added to each existing `*ServiceClient` Rest Client interface, combined in gateway memory — same pattern ADR-013 already established for `ProjectionCalculationEngine`, not a new aggregation style; a domain that's down contributes an error-describing entry instead of failing the whole call.
  - **Contracts:** all four domain contracts (`application/contract/{profile,wealth,health,household}.yaml`) gained the `/v1/errors` path + `ErrorLogResponse` schema, mirrored into `application/web-gateway/src/main/resources/`; `gateway.yaml` gained the four `/v1/console/...` paths (`status`, `services/{name}/start`, `services/{name}/stop`, `errors`) + `ServiceStatusResponse`/`ServiceActionResponse`/`ConsoleErrorsResponse` schemas. `web/src/api/generated.ts` since regenerated (contains the `/v1/console/*` operations/schemas) — used only as a read-only type reference during frontend implementation, not as a runtime import (see frontend bullet below for why).
  - **Verified:** full `./gradlew test` green across `shared`, all 4 domains (domain+ports+adapters), and `web-gateway`, including ArchUnit and the `gateway_resources_must_have_corresponding_test`/`ports_input_interfaces_must_be_referenced_by_a_test_class` rules. Manually confirmed Part A on both platforms: `run-local.ps1 -Service wealth`/`run-local.sh wealth` started only wealth (`status` showed 1/7 UP) and the matching `stop-local ... wealth` cleanly stopped it, everything else untouched. One nuance found during testing, not a regression: pre-existing `@QuarkusTest`+RestAssured tests in household's default `%test` profile (e.g. `GoalResourceTest`) trigger a genuine HTTP round-trip through `ApplicationExceptionMapper`, and the `ErrorLogRecorder` persist attempt intermittently fails there with a Panache "entity not found" error under that specific test harness config — caught and logged at WARN exactly as designed (a persistence failure must never change the HTTP response), so no test failed, but worth a closer look before relying on `error_log` completeness under that profile. The dedicated per-domain `%integration-test`-profiled `ErrorLogPanacheRepositoryTest` (real Postgres round-trip) passed cleanly for all four domains.
  - **(3) Frontend — `web/src/pages/Admin/ApplicationConsole.js` (new, admin-only route `/admin/console`, `requiredRole="admin"`), added to the Admin nav dropdown (`Navigation.js`) alongside Household Setup/Policy Settings.** Renders one card per service from `GET /v1/console/status` (name/kind/port/UP-DOWN badge), Start/Stop buttons wired to the two POST endpoints (disabled while the service is already in that state or an action is in flight, re-fetches status via `queryClient.invalidateQueries` in `onSettled`), and an expandable per-domain error panel (only for `profile`/`wealth`/`health`/`household` — gateway/web have no `error_log` table) sourced from `GET /v1/console/errors`. Uses React Query throughout (ADR-018 — `useQuery` with a 10s `refetchInterval` for status/errors, disabled once a 404 confirms the feature is off; `useMutation` for start/stop), matching `Dashboard.js`'s established pattern. The disabled/404 case renders a plain explanatory card instead of crashing. **Deliberate deviation from the task brief:** the brief assumed the page would consume `web/src/api/generated.ts` directly, but every existing page in this repo actually calls hand-written `fetch()` wrappers in `api/<domain>.js` (the "reality check" convention documented in `FRONTEND_GUIDELINES.md`'s section 5 override note) — `generated.ts` was read only to confirm exact field names/shapes, and a new `web/src/api/console.js` wrapper module was added following that same established convention rather than introducing the first runtime consumer of `generated.ts` unasked. **A real bug was caught and fixed during live verification, not just unit tests:** `ConsoleErrorAggregationService`'s class-level javadoc claims a down domain "contributes an empty array," but the actual code (`fetch()`'s catch block) returns a one-element array shaped `{"error": "Could not reach <domain> service: ..."}` — not the normal `ErrorLogResponse` shape (`error_code`/`http_status`/`message`) the OpenAPI contract documents. Confirmed live against the real gateway with all four domains stopped. `ErrorEntryRow` now renders both shapes (falls back to `entry.error` for the message, shows `SERVICE_UNREACHABLE` in place of a blank/`UNKNOWN_ERROR` code), with a regression test (`ApplicationConsole.test.js`) covering the fallback shape explicitly. **Live-verified** (not just Jest): started the real gateway with `suchika.console.enabled=true` temporarily (reverted to `false` before finishing) plus the real frontend dev server, drove the actual page with a throwaway Playwright script (admin sign-in via the existing demo-auth fallback, navigate to `/admin/console`) — confirmed all 6 services render with their real live status (profile/wealth/health/household DOWN, gateway UP, web DOWN), the wealth error panel expands to show the real `SERVICE_UNREACHABLE` connection-refused text, and Start/Stop disabled states are correct per real service status (Stop disabled on a DOWN service, Start disabled on the UP gateway). Did not click a live Start/Stop action end-to-end (the backend's own `ServiceControlService` blocks synchronously up to 90s per action and would have spawned a real long-running dev process) — instead confirmed both POST endpoints' exact response shape via `curl` directly (`{service, action, status, output}`, matching `ServiceActionResponse`) and confirmed the button wiring/disabled-state logic visually. 13 new Jest tests in `ApplicationConsole.test.js` (status render, loading, 404-disabled, non-404 error, start/stop call the right endpoint + refetch, disabled-while-already-in-that-state, pending label, error panel expand with both response shapes, no error panel for gateway/web, `ProtectedRoute` admin/unauthenticated gating) — full suite 644/644 passing, `lint`/`format:check`/`build` all clean.

**v0.6 key additions (Testing Foundation, re-scoped, all complete):**

- New ArchUnit rule enforcing every `ports.input` use-case interface is referenced by at least one test class — immediately surfaced and closed 3 previously-zero-coverage resources (`AdminResource`, `ProfileResource`, `PhysicalAssetResource`)
- HTTP adapter unit tests: `AccountResource`, `TransactionResource`, `VitalReadingResource`, `DoctorVisitResource`, `AdminResource`, `ProfileResource`, `PhysicalAssetResource`
- Domain entity unit tests: `Profile`, `Admin`, `VitalReading`
- Jest branch coverage gate (real current level: branches 70%, functions 75%, lines 80%, statements 79% — not the aspirational 80% branch figure originally named, which measurement showed wasn't met yet)
- ADR-019: documents `profileId`-as-domain-field as a deliberate ADR-006 trade-off (7 entities across wealth/health/household)
- UX: transaction list pagination (`page`/`size` on `GET /transactions`), doctor visit date-range filter (`from`/`to` on `GET /doctor-visits`), Goals page auto-refresh copy note

**v0.5 key additions (Phases 0-3, all complete):**

- **Phase 0:** `PATCH /v1/vitals/{id}` edit + modal; `PUT /v1/inventory-items/{id}` edit + modal; `is_consumed` flag on inventory items (Q6); `profile_id` threaded through transaction list/dedup (closed a real ADR-006 gap); Reports page net balance bug fixed (was still summing raw `opening_balance`)
- **Phase 1:** React Query adopted for frontend server state (ADR-018, resolves PROP-005); `Dashboard.js` migrated as the reference pattern
- **Phase 2:** Vacation Planner (`/household/vacation-planner`) — trip budget check against liquid savings, vehicle compliance check against trip dates; new gateway package `com.suchika.gateway.vacationplanner`
- **Phase 3:** Consolidated Action Center (`/action-center`) — 12th `ProjectionCalculationEngine` step aggregating upcoming events, vehicle compliance deadlines, and biometric streak gaps (Q30: core 3 vital types, 30-day threshold, per-profile) across all household members

**v0.4 key additions:**

*Error Handling (core v0.4):*
- Malformed CSV rejection: structured error log to `wealth.upload_error_log`; `GET /uploads/{id}/errors` endpoint
- Dedup key fix: 4-field key `(account_id, txn_date, amount, txn_type)` — description excluded
- Frontend upload error panel and skipped-duplicates panel

*Physical Assets — full vertical slice (v0.4.1 patch):*
- `wealth.physical_asset` table with JSONB metadata, registration uniqueness constraint
- Full CRUD API (`/v1/physical-assets`), gateway proxy, frontend page at `/wealth/physical-assets`
- PUC / insurance / road-tax compliance-deadline fields with expiry-status colouring

*Manual Transaction Entry (Q7):*
- `POST /v1/accounts/{accountId}/transactions`; `upload_id` made nullable (`V7` migration)
- `source = MANUAL` in transaction metadata distinguishes from CSV-sourced rows
- Add Transaction modal in frontend Transactions page

*Epic 8 — Automated Wealth Intelligence Engine (Phases 1–4):*
- **Phase 1:** `account.metadata` JSONB column; `PATCH /accounts/{id}/classification` (category, liquidity tier, purpose tag, loan fields, joint owners); `GET /accounts/{id}/balance` (opening + transactions); `computeCategoryValidation` and `computeFamilyNetWorth` gateway steps; ADR-016 (joint accounts) and ADR-017 (family rollup)
- **Phase 2:** Expense category tagging — single (`PATCH /transactions/{id}/category`) and bulk by ID list; 5 `ExpenseCategory` enum values; `TransactionEntity.metadata` JSONB wired end-to-end
- **Phase 3:** `AmortizationCalculator` (pure domain, EMI formula with offset arbitrage); `computeEmiTracking`, `computeLiquidityTiers`, `computeGrowthProjection` gateway steps; loan details form in Accounts UI; `JsonbMetadataUtil` eliminates CPD across 3 entity classes
- **Phase 4:** `policy_settings JSONB` on `profile.admin` (Flyway V3); `PATCH /admins/{id}/policy`; `computeFormulaGoals` (5 formula goals: Debt Crossover, 30-70 Target, Freedom Runway, Insurance Free, Year One); `computeValidation` (4 advisory checks); Admin Policy Settings page; Dashboard goals + validation cards

---

## Service Map

| Service | Port | DB Schema | Start command |
|---|---|---|---|
| profile | 8081 | `profile` | `./gradlew :application:domain:profile:adapters:quarkusDev` |
| wealth | 8082 | `wealth` | `./gradlew :application:domain:wealth:adapters:quarkusDev` |
| health | 8083 | `health` | `./gradlew :application:domain:health:adapters:quarkusDev` |
| household | 8084 | `household` | `./gradlew :application:domain:household:adapters:quarkusDev` |
| web-gateway | 8080 | `projections` | `./gradlew :application:web-gateway:quarkusDev` |
| frontend | 3000 | — | `cd web && npm start` |

**Startup order matters:** profile → wealth/health/household → gateway → frontend.
Frontend talks only to gateway (8080). Never call domain ports directly from React.

---

## Architecture in One Paragraph

Each domain is a Gradle sub-project with three layers: `domain/` (pure Java, zero framework deps), `ports/` (interfaces), `adapters/` (Quarkus/JPA/HTTP). ArchUnit enforces this. The `web-gateway` is a BFF that aggregates domain REST calls and has no DB of its own. All DB queries filtered by `profile_id` (injected in adapters only). No SQL ENUMs — VARCHAR with OpenAPI enum validation.

---

## Key Invariants (breaks build if violated)

1. `domain/` layer: zero `@Inject`, zero JPA, zero HTTP types — ArchUnit test enforces this
2. `profile_id` filter in every DB query — in adapter, never in domain
3. No SQL ENUMs — VARCHAR for all discriminators
4. Never edit a committed Flyway migration — create a new versioned file
5. Frontend never calls domain ports — only gateway at port 8080
6. `web/src/api/generated.ts` is never hand-edited — always `npm run generate:api`

---

## Domain State Files (read before working on a domain)

- [documents/domain-state/profile.md](domain-state/profile.md) — schema, files, open issues
- [documents/domain-state/wealth.md](domain-state/wealth.md) — schema, ADRs, backlog
- [documents/domain-state/health.md](domain-state/health.md) — schema, backlog
- [documents/domain-state/household.md](domain-state/household.md) — calendar events, inventory, goals; projection engine in gateway; v0.3 complete

---

## Agent Protocol

**Before starting any task:**
1. Read this file (CONTEXT_PRIMER.md)
2. Read the relevant `documents/domain-state/<domain>.md`
3. Read the specific source files for your task

**After completing any task:**
Update `documents/domain-state/<domain>.md` — mark done items, add new open issues, update schema if DB changed, update "last updated" date.

---

## Branch & PR Governance

Six governance files in `.github/` (added v0.2, updated v0.4):

| File | Purpose |
|---|---|
| `.github/CODEOWNERS` | `* @ketan` — all files owned by ketan; comment lines removed in v0.4 |
| `.github/pull_request_template.md` | Standard PR description template auto-loaded on PR creation |
| `.github/labeler.yml` | Path-to-label mapping used by the pr-labeler workflow |
| `.github/workflows/branch-name-check.yml` | Branch must match `^[a-zA-Z][a-zA-Z0-9_-]{3,}$` — starts with letter, min 4 chars, letters/digits/hyphens/underscores only. No type-prefix required (changed in v0.4). |
| `.github/workflows/pr-title-lint.yml` | Enforces Conventional Commits format on PR titles (e.g. `feat(wealth): add CSV upload`) |
| `.github/workflows/pr-labeler.yml` | Auto-labels PRs based on changed file paths (domain, frontend, infra, docs, etc.) |

CI workflow (`.github/workflows/ci.yml`) triggers on `main` branch only — dead `master` trigger removed.

---

## Where to Find Things

| Need | Location |
|---|---|
| Architecture rules | `documents/ARCHITECTURE_GUIDELINES.md` |
| Business requirements | `documents/BUSINESS_REQUIREMENTS.md` |
| Roadmap / milestones | `documents/ROADMAP.md` |
| Logging & exceptions | `documents/LOGGING_AND_EXCEPTIONS.md` |
| Frontend guidelines | `documents/FRONTEND_GUIDELINES.md` |
| E2E tests | `documents/E2E_TESTING.md` |
| Scripts / dev commands | `documents/SCRIPTS.md` |
| CI/CD pipeline | `documents/CICD.md` |
| PR governance | `.github/CODEOWNERS`, `.github/workflows/branch-name-check.yml`, `.github/workflows/pr-title-lint.yml` |
| OpenAPI contracts | `application/contract/<domain>.yaml` |
| Gateway contract | `application/contract/gateway.yaml` |
| Flyway migrations | `application/flyway/<domain>/` |
| Canonical code pattern | Copy from profile domain (it was the first) |
| Epic 8 implementation detail (complete) | `documents/domain-state/wealth.md` |
| GraalVM native-image feasibility (deferred spike) | `documents/NATIVE_INVESTIGATION.md` |

## Agent Roster

| Agent | Use for |
|---|---|
| `devops` | Scripts, startup, ports, DB, logs, lnav, CI — anything about running the system |
| `wealth-developer` | Accounts, transactions, CSV upload (port 8082) |
| `health-developer` | Vitals, doctor visits (port 8083) |
| `profile-developer` | Admin, member profiles (port 8081) |
| `household-developer` | Calendar, inventory, goals — v0.3 (port 8084) |
| `quarkus-developer` | Cross-domain backend Java work |
| `react-developer` | Cross-domain frontend work |
| `architect` | New domain design, ADRs, cross-domain patterns |
| `quality-manager` | SonarQube, ArchUnit, test coverage gates |
| `document-writer` | Update docs, SCRIPTS.md, domain-state files |
