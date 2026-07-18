# Logging and Exception Handling Guide

| | |
|---|---|
| **Type** | Guideline |
| **Audience** | Backend developers |
| **Status** | Active |
| **Last updated** | 2026-07-13 |

## Objective

Define how all services must log events and throw exceptions. Both `AppLogger` and the typed exception hierarchy live in `shared/` — every domain service uses them and nothing else.

## Use Cases

- Before writing any logging statement — use the patterns shown here, not raw SLF4J, JBoss Logging, JUL, or `System.out`
- Before throwing an error — pick the right exception type from the table; never throw raw `RuntimeException`
- When adding a new domain service — wire up `AppLogger` and confirm `ApplicationExceptionMapper` is on the classpath

---

## Overview

The Suchika project includes a unified logging and exception handling system in the `shared/` module. This ensures consistent error handling and logging across all domains.

## The 4 Logging Conventions — and no others

Exactly 4 conventions are used project-wide. **There is no DEBUG level anywhere** — `AppLogger` has no `debug()` method, and an ArchUnit rule (`DomainRulesTest.app_logger_must_not_declare_a_debug_method` / `application_code_must_not_call_debug_methods`) fails the build if one is reintroduced or if any code calls a method named `debug(...)`.

| Convention | `AppLogger` method | Underlying severity | When to use |
|---|---|---|---|
| INFO | `AppLogger.info(...)` | INFO | Normal application events worth recording — request handled, resource created, upload processed |
| WARNING | `AppLogger.warn(...)` | WARN | Recoverable problems, client errors (4xx) — bad input, not-found, conflicts |
| ERROR | `AppLogger.error(...)` | ERROR | Unrecoverable failures, server errors (5xx) — unexpected exceptions, failed writes |
| HEALTH | `AppLogger.health(...)` | INFO (dedicated category) | Service lifecycle — startup, shutdown, health-probe activity |

Quarkus's underlying log level enum is literally `WARN`, not `WARNING` — "WARNING" is this project's name for the convention, `warn()`/`WARN` is the method/level name. Don't be thrown by the mismatch.

### `AppLogger.health(...)` — why a category, not a new Level

There is no built-in `HEALTH` severity in JBoss Logging / `java.util.logging`, and this project does not invent a custom `java.util.logging.Level` for it (that requires custom `Level` subclassing that most log handlers/aggregators don't understand). Instead, `AppLogger.health(...)` logs at **INFO severity** through a **separate logger category**, `com.suchika.health` (distinct from the category `io.quarkus.logging.Log` otherwise resolves for the rest of `AppLogger`, which is `com.suchika.shared.logging.AppLogger` — confirmed empirically, since `Log` resolves the *calling class*, and every other `AppLogger` method call originates from inside `AppLogger.java` itself).

Every `application.properties` in this repo already renders `%c{3.}` (the category) in its log format, so HEALTH events are filterable in `lnav` / log aggregation by category, with no new Level to teach every tool about.

**Do not confuse this with the "health" domain** (vital readings, doctor visits — `com.suchika.health.*` packages). The `AppLogger.health(...)` category is about *service* health — lifecycle, startup/shutdown, health-probe activity — a homonym, not the same concept. Domain business events in the health *domain* still use `AppLogger.info/warn/error` like every other domain.

### Basic Usage

```java
import com.suchika.shared.logging.AppLogger;

// Info logging
AppLogger.info("Transaction uploaded successfully");
AppLogger.info("Transaction uploaded: %s", transactionId);

// Warning logging — recoverable / client-caused problems
AppLogger.warn("Duplicate transaction detected");

// Error logging — unrecoverable / server-caused failures
AppLogger.error("Failed to process request", exception);

// Health logging — service lifecycle, not domain "health" data
AppLogger.health("Service started on port %s", port);
```

### Advantages

- Single point of change for logging configuration
- Consistent log formatting across all domains
- Integrates with Quarkus logging system
- Locks the "exactly 4 conventions, no DEBUG" rule in at compile time via ArchUnit

---

## Exception Handling

Use domain-specific exceptions that extend `ApplicationException`. Each exception maps to a specific HTTP status code.

### Available Exceptions

| Exception | Status | Error Code | Use Case |
|---|---|---|---|
| `BadRequestException` | 400 | BAD_REQUEST | Invalid input, malformed request |
| `UnauthorizedException` | 401 | UNAUTHORIZED | Missing/invalid authentication |
| `ForbiddenException` | 403 | FORBIDDEN | User lacks permissions |
| `NotFoundException` | 404 | NOT_FOUND | Resource does not exist |
| `NotAcceptableException` | 406 | NOT_ACCEPTABLE | Unsupported media type |
| `ConflictException` | 409 | CONFLICT | Resource already exists, state conflict |
| `InternalServerException` | 500 | INTERNAL_SERVER_ERROR | Unexpected server error |
| `NotImplementedException` | 501 | NOT_IMPLEMENTED | Feature not yet available |

### Usage Examples

```java
import com.suchika.shared.exception.*;

// Simple error
throw new NotFoundException("Transaction not found");

// With details
throw new NotFoundException(
    "Transaction not found", 
    "Transaction ID: 12345"
);

// With cause
try {
    // database operation
} catch (Exception e) {
    throw new InternalServerException("Failed to save transaction", e);
}
```

### Error Response Format

All exceptions automatically return a JSON response:

```json
{
    "status": 404,
    "errorCode": "NOT_FOUND",
    "message": "Transaction not found",
    "details": "Transaction ID: 12345",
    "timestamp": 1716696000000
}
```

### Exception -> Log Level Mapping

`ApplicationExceptionMapper` converts every `ApplicationException` to its HTTP response AND logs it, split by status code (fixed 2026-07 — previously it logged everything at WARN regardless of status):

| Status range | `AppLogger` call |
|---|---|
| 4xx (client error) | `AppLogger.warn(...)` |
| 5xx (server error) | `AppLogger.error(...)` |

`IllegalArgumentExceptionMapper` (for domain-layer validating factories, e.g. `Goal.create(...)`) always maps to 400, so it always logs at `warn` — already consistent with the table above.

The mapper also:
- Converts exceptions to HTTP responses
- Sets appropriate status codes
- Includes timestamp for tracking

---

## Integration in Controllers

```java
import com.suchika.shared.logging.AppLogger;
import com.suchika.shared.exception.*;

@Path("/v1/transactions")
public class TransactionController {

    @GET
    @Path("/{id}")
    public TransactionDTO getTransaction(@PathParam("id") String id) {
        Transaction transaction = repository.findById(id);
        if (transaction == null) {
            throw new NotFoundException("Transaction not found", "ID: " + id);
        }
        
        AppLogger.info("Transaction retrieved successfully: %s", id);
        return mapToDTO(transaction);
    }

    @POST
    public TransactionDTO createTransaction(TransactionRequest request) {
        if (request.getAmount() <= 0) {
            throw new BadRequestException("Amount must be positive");
        }
        
        Transaction transaction = repository.save(request);
        AppLogger.info("Transaction created: %s", transaction.getId());
        return mapToDTO(transaction);
    }
}
```

Note there's no `AppLogger.debug(...)` call at method entry — that pattern is gone. Log the meaningful outcome (INFO) or let `ApplicationExceptionMapper` log the failure (WARNING/ERROR); don't log routine "entering method X" traces.

---

## Logging Configuration

Configure logging in `application.properties`. This matches what's actually deployed in all 5 services (profile/wealth/health/household/web-gateway) as of 2026-07-13:

```properties
# Console format — applies to all profiles (dev/test/prod), not dev-only
quarkus.log.console.format=%d{yyyy-MM-dd HH:mm:ss\,SSS} %-5p [<domain>] [%c{3.}] (%t) %s%e%n

# Runtime log file — read by lnav. Dev mode only.
%dev.quarkus.log.file.enable=true
%dev.quarkus.log.file.path=${user.home}/.suchika/logs/<domain>.log
%dev.quarkus.log.file.level=INFO
%dev.quarkus.log.file.format=%d{yyyy-MM-dd HH:mm:ss\,SSS} %-5p [<domain>] [%c{3.}] (%t) %s%e%n
%dev.quarkus.log.file.rotation.max-file-size=10M
%dev.quarkus.log.file.rotation.max-backup-index=2
```

`%c{3.}` renders the logger category (abbreviated to 3 segments) — this is what makes `AppLogger.health(...)`'s dedicated `com.suchika.health` category filterable in `lnav` without needing a custom log Level.

The file-appender level is `INFO`, matching the "no DEBUG" rule — it used to be hardcoded `DEBUG` in every service despite zero `debug()` call sites existing anywhere in the codebase; that mismatch is what this doc revision fixes.

---

## Best Practices

1. **Use appropriate exception types** - Don't throw generic exceptions
2. **Log at the right level** - INFO for normal events, WARNING for 4xx/recoverable problems, ERROR for 5xx/unrecoverable failures, HEALTH for service lifecycle. No DEBUG, ever.
3. **Include context** - Use the details parameter in exceptions for debugging
4. **Avoid sensitive data** - Never log passwords, tokens, or PII
5. **Consistent naming** - Use domain prefixes in error codes if needed (e.g., WEALTH_INVALID_ACCOUNT)
6. **Never use a raw logger** - No `org.slf4j.Logger`, `org.jboss.logging.Logger`, or `java.util.logging.Logger` outside `AppLogger.java` itself. Enforced by ArchUnit (`DomainRulesTest.application_code_must_not_use_raw_loggers`).

---

## Future Enhancements

- Custom error codes per domain
- OpenAPI schema generation from exceptions
- Distributed tracing integration
- Centralized log aggregation
