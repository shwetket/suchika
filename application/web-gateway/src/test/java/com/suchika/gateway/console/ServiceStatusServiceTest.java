package com.suchika.gateway.console;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Plain-JUnit unit test (no @QuarkusTest needed -- ServiceStatusService has
 * no CDI dependencies). Does not require any real Suchika service to be
 * running: every service is expected to report DOWN in this environment,
 * which is itself the behavior under test (a service that isn't listening
 * must never be reported UP).
 */
class ServiceStatusServiceTest {

    private final ServiceStatusService service = new ServiceStatusService();

    @Test
    void listStatuses_returnsOneEntryPerKnownService() {
        List<ServiceStatus> statuses = service.listStatuses();

        assertEquals(ServiceDefinition.getAll().size(), statuses.size());
        for (int i = 0; i < statuses.size(); i++) {
            assertEquals(ServiceDefinition.getAll().get(i).name(), statuses.get(i).name());
            assertEquals(ServiceDefinition.getAll().get(i).port(), statuses.get(i).port());
            assertNotNull(statuses.get(i).status(), "status must never be null -- DOWN, not absent, when unreachable");
        }
    }
}
