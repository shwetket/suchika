# /unit-test — Generate and Run Domain Layer Unit Tests

Write or fix unit tests for the domain class specified in $ARGUMENTS. Format: `<domain>/<ClassName>` (e.g. `wealth/CreateAccountCommand`, `health/VitalReading`). If only a domain name is given, audit all domain classes for missing test coverage.

## Step 1 — Read context
- Read `documents/domain-state/<domain>.md` for schema and design decisions
- Read the target domain class(es)
- Read existing tests in `application/domain/<domain>/domain/src/test/` to match the pattern

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
