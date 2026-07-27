package com.suchika.wealth.adapters.http;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.wealth.adapters.http.dto.ListUploadsResponse;
import com.suchika.wealth.adapters.http.dto.StatementUploadResponse;
import com.suchika.wealth.adapters.http.dto.UploadErrorLogResponse;
import com.suchika.wealth.adapters.http.dto.UploadStatementRequest;
import com.suchika.wealth.ports.input.StatementUploadUseCase;
import com.suchika.wealth.ports.input.UploadResult;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/v1/accounts/{account_id}/uploads")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class StatementUploadResource {

    private final StatementUploadUseCase useCase;

    public StatementUploadResource(StatementUploadUseCase useCase) {
        this.useCase = useCase;
    }

    @POST
    public Response uploadStatement(
            @PathParam("account_id") UUID accountId,
            UploadStatementRequest request) {
        if (request == null) throw new BadRequestException("Request body is required");
        if (request.fileName == null || request.fileName.isBlank()) {
            throw new BadRequestException("file_name is required");
        }
        if (request.csvContent == null || request.csvContent.isBlank()) {
            throw new BadRequestException("csv_content is required");
        }
        UploadResult result = useCase.uploadStatement(accountId, request.fileName, request.csvContent);
        return Response.status(201).entity(StatementUploadResponse.from(result)).build();
    }

    @GET
    public Response listUploads(@PathParam("account_id") UUID accountId) {
        List<StatementUploadResponse> uploads = useCase.listUploads(accountId)
                .stream().map(StatementUploadResponse::from).toList();
        return Response.ok(new ListUploadsResponse(uploads)).build();
    }

    @DELETE
    @Path("/{upload_id}")
    public Response rollbackUpload(
            @PathParam("account_id") UUID accountId,
            @PathParam("upload_id") UUID uploadId) {
        useCase.rollbackUpload(uploadId);
        return Response.noContent().build();
    }

    @GET
    @Path("/{upload_id}/errors")
    public Response getUploadErrors(
            @PathParam("account_id") UUID accountId,
            @PathParam("upload_id") UUID uploadId) {
        List<UploadErrorLogResponse> errors = useCase.getUploadErrors(uploadId)
                .stream().map(UploadErrorLogResponse::from).toList();
        return Response.ok(errors).build();
    }
}
