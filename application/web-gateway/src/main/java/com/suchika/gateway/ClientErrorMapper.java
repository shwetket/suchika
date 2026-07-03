package com.suchika.gateway;

import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

/**
 * Forwards error responses from downstream Rest Client calls (domain services) back to the
 * caller as-is. {@code WebApplicationException} is also thrown by RESTEasy itself for the
 * gateway's own unmatched routes and framework endpoints (e.g. {@code /q/health}) — those
 * responses carry no readable string entity, so {@code readEntity} is guarded here rather than
 * letting a ProcessingException escape as an unrelated 500.
 */
@Provider
public class ClientErrorMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        Response upstream = exception.getResponse();
        if (!upstream.hasEntity()) {
            return Response.status(upstream.getStatus()).build();
        }
        try {
            String body = upstream.readEntity(String.class);
            return Response.status(upstream.getStatus())
                    .entity(body)
                    .type(MediaType.APPLICATION_JSON)
                    .build();
        } catch (ProcessingException e) {
            return Response.status(upstream.getStatus()).build();
        }
    }
}
