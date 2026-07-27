# /integration-test — Generate and Run Adapter Layer Integration Tests

Write or fix integration tests for the adapter class specified in $ARGUMENTS. Format: `<domain>/<ResourceOrRepository>` (e.g. `wealth/AccountResource`, `health/VitalRepository`). If only a domain is given, audit all adapter tests for that domain.

## Step 1 — Read context
- Read `documents/domain-state/<domain>.md` for schema and FK constraints
- Read the target adapter class(es)
- Read existing adapter tests in `application/domain/<domain>/adapters/src/test/` to match the pattern

## Step 2 — Rules for Adapter Integration Tests

### Framework
- `@QuarkusTest` annotation on test class
- **Real PostgreSQL via Testcontainers** — no H2, no mocked repositories
- `@TestHTTPEndpoint(<Resource>.class)` for HTTP resource tests
- `@InjectMock @RestClient` for gateway tests that call domain REST clients (not actual HTTP calls)
- Use `RestAssured` for HTTP resource tests

### Dependencies (already in build.gradle.kts — verify they're present)
```kotlin
testImplementation("io.quarkus:quarkus-junit5")
testImplementation("io.rest-assured:rest-assured")
testImplementation("io.quarkus:quarkus-test-postgresql")  // Testcontainers
```

### What to test for every HTTP Resource (JAX-RS)
- [ ] `GET` list — returns 200 + array; empty if no data
- [ ] `GET` list — `profile_id` scoping: seed two profiles, verify each only sees own data
- [ ] `POST` create — returns 201 + created entity with ID
- [ ] `POST` create — 400 for missing required fields
- [ ] `GET` by id — returns 200 + correct entity
- [ ] `GET` by id — returns 404 for unknown id
- [ ] `PUT` update — returns 200 + updated fields
- [ ] `DELETE` — returns 204; subsequent `GET` returns 404 or shows deactivated

### What to test for every Repository (Panache)
- [ ] `save()` persists all fields correctly
- [ ] `findById(id, profileId)` returns empty for wrong profile (scoping test)
- [ ] `findAll(profileId)` returns only records for that profile
- [ ] `delete()` removes the record

### Test data setup
```java
@BeforeEach
void setUp() {
    // Use Panache.withTransaction or EntityManager to seed test data
    // Each test method gets a clean DB state (use @Transactional or truncate)
}
```

### profile_id scoping test (mandatory for every domain)
```java
@Test
void list_differentProfiles_scopedCorrectly() {
    UUID profile1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    UUID profile2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    // Seed one record for each profile
    // GET with profile1 → returns only profile1's record
    // GET with profile2 → returns only profile2's record
}
```

## Step 3 — Write Tests

Path: `application/domain/<domain>/adapters/src/test/java/com/suchika/<domain>/adapters/<ClassName>Test.java`

## Step 4 — Run and Verify

```
./gradlew :application:domain:<domain>:adapters:test
```

Report:
- Tests written and what they cover
- Pass/fail count
- Any missing coverage (especially profile_id scoping)
