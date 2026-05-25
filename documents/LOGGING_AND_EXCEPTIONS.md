# Logging and Exception Handling Guide

## Overview

The Suchika project includes a unified logging and exception handling system in the `shared/` module. This ensures consistent error handling and logging across all domains.

## Logger Usage

The `AppLogger` class provides a static interface for logging throughout the application.

### Basic Usage

```java
import com.suchika.shared.logging.AppLogger;

// Info logging
AppLogger.info("Transaction uploaded successfully");

// Debug logging
AppLogger.debug("Processing transaction ID: %s", transactionId);

// Warning logging
AppLogger.warn("Duplicate transaction detected");

// Error logging
AppLogger.error("Failed to process request", exception);
```

### Advantages

- Single point of change for logging configuration
- Consistent log formatting across all domains
- Integrates with Quarkus logging system
- Supports JSON structured logging (configured in `application.properties`)

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

The `ApplicationExceptionMapper` automatically:
- Converts exceptions to HTTP responses
- Sets appropriate status codes
- Logs warnings for all errors
- Includes timestamp for tracking

---

## Integration in Controllers

```java
import com.suchika.shared.logging.AppLogger;
import com.suchika.shared.exception.*;

@Path("/api/v1/transactions")
public class TransactionController {

    @GET
    @Path("/{id}")
    public TransactionDTO getTransaction(@PathParam("id") String id) {
        AppLogger.debug("Fetching transaction: %s", id);
        
        Transaction transaction = repository.findById(id);
        if (transaction == null) {
            throw new NotFoundException("Transaction not found", "ID: " + id);
        }
        
        AppLogger.info("Transaction retrieved successfully: %s", id);
        return mapToDTO(transaction);
    }

    @POST
    public TransactionDTO createTransaction(TransactionRequest request) {
        AppLogger.debug("Creating new transaction");
        
        if (request.getAmount() <= 0) {
            throw new BadRequestException("Amount must be positive");
        }
        
        Transaction transaction = repository.save(request);
        AppLogger.info("Transaction created: %s", transaction.getId());
        return mapToDTO(transaction);
    }
}
```

---

## Logging Configuration

Configure logging in `application.properties`:

```properties
# Log level
quarkus.log.level=INFO
quarkus.log.category."com.suchika".level=DEBUG

# JSON structured logging (optional)
quarkus.log.console.json=true
```

---

## Best Practices

1. **Use appropriate exception types** - Don't throw generic exceptions
2. **Log at the right level** - Info for important events, Debug for detailed traces, Error for failures
3. **Include context** - Use the details parameter in exceptions for debugging
4. **Avoid sensitive data** - Never log passwords, tokens, or PII
5. **Consistent naming** - Use domain prefixes in error codes if needed (e.g., WEALTH_INVALID_ACCOUNT)

---

## Future Enhancements

- Custom error codes per domain
- OpenAPI schema generation from exceptions
- Distributed tracing integration
- Centralized log aggregation
