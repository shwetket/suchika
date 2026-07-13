package com.suchika.household.adapters.http;

import com.suchika.shared.errorlog.ErrorLogUseCase;
import com.suchika.sharedadapter.errorlog.AbstractErrorLogResource;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * Phase 4 Application Console (ADR-023): exposes this domain's own
 * {@code error_log} table for the admin-only console to read. No
 * profile_id scoping (ADR-006 governs member-owned business data; this is
 * an operational/audit log, same precedent as wealth.upload_error_log).
 *
 * <p>Since/limit parsing and pagination logic lives once in {@link
 * AbstractErrorLogResource} (2026-07-13 ADR-023 revision) -- this class
 * only binds the domain's own {@link ErrorLogUseCase} implementation and
 * carries the {@code @Path}/{@code @Produces} annotations Quarkus needs to
 * discover it, since discovery only ever has to find this already-indexed
 * concrete class in this domain's own adapters module.
 */
@Path("/v1/errors")
@Produces(MediaType.APPLICATION_JSON)
public class ErrorLogResource extends AbstractErrorLogResource {

    private final ErrorLogUseCase errorLogUseCase;

    public ErrorLogResource(ErrorLogUseCase errorLogUseCase) {
        this.errorLogUseCase = errorLogUseCase;
    }

    @Override
    protected ErrorLogUseCase useCase() {
        return errorLogUseCase;
    }
}
