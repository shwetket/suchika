package com.suchika.gateway;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ClientErrorMapper implements ExceptionMapper<WebApplicationException> {

    @Override
    public Response toResponse(WebApplicationException exception) {
        Response upstream = exception.getResponse();
        String body = upstream.readEntity(String.class);
        return Response.status(upstream.getStatus())
                .entity(body)
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
