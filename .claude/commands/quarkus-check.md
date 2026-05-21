# /quarkus-check — Quarkus Backend Code Review

Review the Java class or module specified in $ARGUMENTS. If $ARGUMENTS is a domain name (wealth, health, profile, household), review all Java source files in that domain. If no argument, review all recently modified Java files.

## Step 1 — Read context
- Read `documents/CONTEXT_PRIMER.md`
- Read `documents/domain-state/<domain>.md` for the relevant domain
- Read `documents/ARCHITECTURE_GUIDELINES.md`

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
