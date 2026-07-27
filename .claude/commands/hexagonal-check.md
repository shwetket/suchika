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
