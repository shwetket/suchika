---
name: household-developer
description: Household domain specialist for Suchika. Use for all backend and frontend work scoped to the household domain — calendar events, inventory items, goals, and task tracking. This domain is NOT started yet (v0.3). Read the domain-state file before asking any questions — the planned schema and constraints are already defined there.
---

Role: Full-stack developer for the Household domain (port 8084).

## Bootstrap — Read Before Any Work

1. `documents/CONTEXT_PRIMER.md` — 2-min project snapshot
2. `documents/domain-state/household.md` — planned schema, open blockers, nothing built yet
3. `documents/ARCHITECTURE_GUIDELINES.md` — hexagonal rules enforced by ArchUnit
4. `application/domain/profile/` — copy the canonical structure from this domain

---

## Domain Context

**Status:** NOT STARTED. v0.3 work item.
**DB schema:** `household` — planned tables: `calendar_event`, `inventory_item`, `goal`

**Nothing exists yet for this domain** except:
- The Quarkus service skeleton on port 8084
- Stub frontend pages in `web/src/pages/Household/` (Calendar.js, Inventory.js)
- No API contract file — create `application/contract/household.yaml` first

**First tasks when starting v0.3:**
1. Create `application/contract/household.yaml`
2. Create `application/flyway/household/V1__create_calendar_event.sql`
3. Build domain layer (Calendar Event entity, use cases)
4. Build adapter layer (Panache repo, JAX-RS resource)
5. Update frontend stub pages with real API calls

---

## Architecture Rules (Non-Negotiable)

- Profile must start before household — household Flyway migrations will reference `profile.profile`.
- `domain/` has zero framework deps — no `@Inject`, no JPA, no HTTP types. ArchUnit enforces this.
- `profile_id` filter injected in adapter layer only, never in domain.
- No SQL ENUMs — VARCHAR for all discriminators.
- `end_date >= start_date` for calendar events: this is a business-rule CHECK constraint — keep it in DB.
- Never edit a committed Flyway migration — add a new versioned file.
- After any contract change: `cd web && npm run generate:api`.
- All logging via `AppLogger`. All exceptions via `shared/exception/` hierarchy.
- Frontend never calls domain ports — only gateway at port 8080.

---

## Canonical Pattern — Copy From Profile Domain

When building household, use profile domain as the template:
- Copy the hexagonal layer structure verbatim
- Use the same package naming: `com.suchika.household.domain.*` / `.ports.input.*` / `.ports.output.*` / `.adapters.*`
- Copy test patterns from `application/domain/profile/`

---

## Testing (mandatory)

**Java:** Domain layer — plain JUnit 5. Adapter layer — Testcontainers + real PostgreSQL.
**React:** Jest + React Testing Library. Cover: render, loading state, error state.

---

## Running Things — Use devops agent or these standard commands

```powershell
. .\scripts\dev-aliases.ps1
dp && dho              # start profile first, then household
./gradlew :application:domain:household:adapters:test
lnav-dev household     # watch household runtime logs
```

For anything operational (scripts, ports, DB, logs) — ask the `devops` agent.

## Completion Checklist

```
Before starting:
□ Read documents/domain-state/household.md carefully
□ Create application/contract/household.yaml

Per feature:
1. Flyway migration (new versioned file)
2. Domain entity + use case
3. Port interface
4. Adapter (Panache repo + JAX-RS resource)
5. Unit tests (JUnit5)
6. Adapter tests (Testcontainers)
7. Frontend page implementation
8. Jest tests
9. ./gradlew :application:domain:household:adapters:test
10. cd web && npm run test:ci && npm run build
11. sonar-scan — zero new issues

After every feature:
□ Update documents/domain-state/household.md
```

---

## Self-Update Protocol

When you finish work, update `documents/domain-state/household.md`:
- Change status of completed items from 🔲 to ✅
- Add any new issues or design decisions discovered
- Update schema table with actual final columns (may differ from planned)
- Update "Last updated" date to today


# /domain-status — Show Current State of All Domains

Read all domain-state files and CONTEXT_PRIMER, then report the complete project status. If $ARGUMENTS is a domain name, show only that domain in detail.


## What to read
1. `documents/CONTEXT_PRIMER.md`
2. `documents/domain-state/profile.md`
3. `documents/domain-state/wealth.md`
4. `documents/domain-state/health.md`
5. `documents/domain-state/household.md`
6. `documents/ROADMAP.md` (for upcoming milestones)


## Report Format

```
=== Suchika Project Status ===
Current version: v0.X | Date: YYYY-MM-DD

PROFILE  (port 8081) — [status]
  ✅ Done: <list complete items>
  🔲 Next: <list pending items>
  ⚠️  Issues: <list any known problems>

WEALTH   (port 8082) — [status]
  ✅ Done: ...
  🔲 Next: ...
  ⚠️  Issues: ...

HEALTH   (port 8083) — [status]
  ✅ Done: ...
  🔲 Next: ...
  ⚠️  Issues: ...

HOUSEHOLD (port 8084) — [status]
  ✅ Done: ...
  🔲 Next: ...
  ⚠️  Issues: ...

QUALITY GATES
  Tests:    X passing / X failing
  Coverage: X% (target: 80%)
  Sonar:    X open issues (target: 0)

NEXT MILESTONE: v0.X — <focus area>
  Features needed: <list>
```


## If $ARGUMENTS is a domain name

Show full detail for that domain:
- Complete schema tables (all columns)
- All API endpoints
- All key files with paths
- Full open issues list
- Design decisions / ADRs
- What to build next (specific files, migrations, steps)


# /hexagonal-check — Hexagonal Architecture Compliance Audit

Audit the domain specified in $ARGUMENTS (wealth, health, profile, or household). If no argument, audit all four domains.


## Step 1 — Read context
- Read `documents/CONTEXT_PRIMER.md`
- Read `documents/ARCHITECTURE_GUIDELINES.md` in full
- Read `documents/ARCHITECTURE_DECISIONS.md` for current ADRs


## Step 2 — Verify Package Structure

Each domain must have exactly this structure:
```
application/domain/<domain>/
├── domain/src/main/java/com/suchika/<domain>/domain/
│   ├── <Entity>.java              ← pure Java entities, no annotations
│   └── <UseCaseImpl>.java         ← implements port input interface
├── ports/src/main/java/com/suchika/<domain>/ports/
│   ├── input/<UseCase>.java       ← interface only, no implementation
│   └── output/<Repository>.java   ← interface only, no implementation
└── adapters/src/main/java/com/suchika/<domain>/adapters/
    ├── http/<Entity>Resource.java  ← JAX-RS controller
    ├── http/dto/                   ← request/response DTOs
    └── persistence/<Entity>Repository.java ← Panache implementation
```

Check:
- [ ] Package names match: `com.suchika.<domain>.domain.*` / `.ports.input.*` / `.ports.output.*` / `.adapters.*`
- [ ] No extra layers outside this structure
- [ ] `domain/` sub-project has zero dependency on `adapters/` sub-project in `build.gradle.kts`


## Step 3 — Verify Layer Isolation


### domain/ layer must NOT contain:
- [ ] `@Inject`, `@ApplicationScoped`, `@RequestScoped`, `@Singleton`
- [ ] `jakarta.persistence.*` (Entity, Column, Table, etc.)
- [ ] `jakarta.ws.rs.*` (GET, POST, Path, etc.)
- [ ] `jakarta.transaction.*`
- [ ] `io.quarkus.*`
- [ ] `org.jboss.resteasy.*`
- [ ] Any HTTP type (`Response`, `Request`, etc.)
- [ ] Any logging framework directly (use AppLogger from shared/)


### ports/ layer must NOT contain:
- [ ] Any implementation (only interfaces)
- [ ] Framework annotations
- [ ] DB types (EntityManager, PanacheRepository, etc.)


### adapters/ layer MAY contain:
- [ ] `@ApplicationScoped`, `@Inject`
- [ ] Panache repositories and entities
- [ ] JAX-RS annotations
- [ ] `profile_id` injection and filtering
- [ ] DTO classes


## Step 4 — Verify ArchUnit Test Coverage
Read `shared/src/test/java/.../DomainRulesTest.java`:
- [ ] Test exists for each domain
- [ ] Rules cover: no framework deps in domain, no upward dependencies
Run: `./gradlew :shared:test`


## Step 5 — Verify Cross-Domain Rules
- [ ] No domain imports classes from another domain
- [ ] Cross-domain data flows only through REST calls via gateway or domain REST client
- [ ] No shared DB tables (each domain owns its schema)
- [ ] FKs to `profile.profile(id)` are one-way only — profile never imports from other domains


## Step 6 — Report

**Violations found:**
```
DOMAIN | LAYER | FILE:LINE — Rule violated
```

**Compliant domains:** List domains with zero violations.

**ADR recommendations:** If a pattern violation is systemic, propose an ADR entry for `documents/ARCHITECTURE_DECISIONS.md`.

After any fixes: `./gradlew :shared:test` must pass (ArchUnit).


# /quarkus-check — Quarkus Backend Code Review

Review the Java class or module specified in $ARGUMENTS. If $ARGUMENTS is a domain name (wealth, health, profile, household), review all Java source files in that domain. If no argument, review all recently modified Java files.


## Step 2 — Audit against these rules


### Hexagonal Layer Compliance
- [ ] `domain/` classes: zero `@Inject`, zero `jakarta.persistence.*`, zero HTTP types (`@GET`, `Response`, etc.)
- [ ] `ports/` interfaces: zero framework annotations — only Java interfaces with method signatures
- [ ] `adapters/` classes: Quarkus/Panache/JAX-RS allowed here only
- [ ] No upward dependency: adapter → port → domain (never reversed)
- [ ] ArchUnit will catch violations — run `./gradlew :shared:test` to verify


### Code Quality (SonarQube Java Rules)
- [ ] No `System.out.println` — use `AppLogger` from `shared/`
- [ ] No empty catch blocks — handle or rethrow via `shared/exception/` hierarchy
- [ ] No unused variables, fields, or imports
- [ ] No magic numbers — use named constants or enums
- [ ] No raw types (e.g. `List` not `List<String>`)
- [ ] String comparison `.equals()` not `==`
- [ ] Resources closed with try-with-resources
- [ ] No `throws Exception` — typed exceptions only
- [ ] Cognitive complexity ≤ 15 per method — extract if deeper
- [ ] No duplicated code blocks — extract to shared helper
- [ ] `final` on fields that never change after construction
- [ ] Public methods return `Optional<T>` or explicit null check — never return `null` from public API


### Database / Persistence Rules
- [ ] Every DB query filters by `profile_id` — adapter layer only, never domain
- [ ] No SQL ENUMs — VARCHAR columns for all discriminator values
- [ ] Flyway: any schema change = new versioned file in `application/flyway/<domain>/`; never edit committed migration
- [ ] FK to `profile.profile(id)` — never to `profile.admin`


### API Contract Rules
- [ ] API paths start with `/api/v1/`
- [ ] Ports: profile=8081, wealth=8082, health=8083, household=8084, gateway=8080 — unchanged
- [ ] After any `application/contract/<domain>.yaml` change: `cd web && npm run generate:api`


### Logging & Exceptions
- [ ] `AppLogger.info()` / `.warn()` / `.error()` — not SLF4J or Log4J directly
- [ ] Exceptions thrown are from `shared/exception/` hierarchy: `NotFoundException`, `BadRequestException`, `ConflictException`, etc.
- [ ] `ApplicationExceptionMapper` handles conversion to HTTP response — don't return error ResponseEntity manually


## Step 3 — Report
```
FILE:LINE — Rule violated — What to change
```
Total issue count per category. If zero, say "Quarkus check clean."


## Step 4 — Fix
Fix all issues. Then verify:
```
./gradlew :application:domain:<domain>:domain:test
./gradlew :application:domain:<domain>:adapters:test
./gradlew :shared:test
```


# /unit-test — Generate and Run Domain Layer Unit Tests

Write or fix unit tests for the domain class specified in $ARGUMENTS. Format: `<domain>/<ClassName>` (e.g. `wealth/CreateAccountCommand`, `health/VitalReading`). If only a domain name is given, audit all domain classes for missing test coverage.


## Step 2 — Rules for Domain Unit Tests


### Framework
- **JUnit 5** only — no Quarkus test harness, no `@QuarkusTest`
- Instantiate with `new` — no `@Inject`, no CDI
- No mocking framework needed for pure domain logic — if you're mocking, the domain class has too many deps
- Use Mockito only for mocking the output port (repository) interface


### What to test for every domain class
**Entities / Value Objects:**
- [ ] Constructor validates required fields — throws `BadRequestException` for null/blank/invalid
- [ ] Getter behavior is correct
- [ ] Business rules enforced (e.g. `amount >= 0`, `endDate >= startDate`)

**Use Case Implementations (input port impls):**
- [ ] Happy path — correct output returned
- [ ] Not found case — throws `NotFoundException`
- [ ] Conflict case — throws `ConflictException`
- [ ] Validation failures — throws `BadRequestException`
- [ ] `profile_id` scoping correct (mock the repository, verify call args)

**Commands / DTOs in domain layer:**
- [ ] All field combinations (required/optional)
- [ ] Immutability — fields are `final`


### Test naming convention
```java
@Test
void <methodName>_<scenario>_<expectedResult>() { }

// Examples:
void createAccount_validCommand_returnsAccount()
void createAccount_nullName_throwsBadRequest()
void getAccount_unknownId_throwsNotFound()
```


### Test structure (AAA)
```java
@Test
void methodName_scenario_result() {
    // Arrange
    var command = new CreateAccountCommand(...);
    when(mockRepo.findById(id)).thenReturn(Optional.of(account));

    // Act
    var result = useCase.execute(command);

    // Assert
    assertThat(result.name()).isEqualTo("expected");
}
```


## Step 3 — Write Tests

Write tests to the path: `application/domain/<domain>/domain/src/test/java/com/suchika/<domain>/domain/<ClassName>Test.java`

Aim for:
- 100% branch coverage on pure domain logic
- Every public method has at least one happy-path test
- Every validation rule has a negative test


## Step 4 — Run and Verify

```
./gradlew :application:domain:<domain>:domain:test
```

Tests must pass. Report:
- Number of tests written
- Pass/fail count
- Any coverage gaps still remaining
