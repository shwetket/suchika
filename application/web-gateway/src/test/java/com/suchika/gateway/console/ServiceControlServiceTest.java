package com.suchika.gateway.console;

import com.suchika.shared.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Plain-JUnit unit test for the validation path only. Deliberately does NOT
 * exercise the real start()/stop() process-launching path for a KNOWN service
 * -- that would actually shell out to run-local.ps1/.sh and start/stop a real
 * Suchika service as a side effect of running the test suite. The
 * known-service happy path is covered by ConsoleResourceTest via
 * @InjectMock'd ServiceControlService instead.
 */
class ServiceControlServiceTest {

    private final ServiceControlService service = new ServiceControlService(Optional.empty());

    @Test
    void start_unknownService_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> service.start("not-a-real-service"));
    }

    @Test
    void stop_unknownService_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> service.stop("not-a-real-service"));
    }
}
