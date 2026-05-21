package com.suchika.wealth.adapters.http;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.wealth.adapters.http.dto.ListTransactionsResponse;
import com.suchika.wealth.adapters.http.dto.TransactionResponse;
import com.suchika.wealth.domain.TxnType;
import com.suchika.wealth.ports.input.TransactionUseCase;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Path("/v1/accounts/{account_id}/transactions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class TransactionResource {

    private final TransactionUseCase useCase;

    public TransactionResource(TransactionUseCase useCase) {
        this.useCase = useCase;
    }

    @GET
    public Response listTransactions(
            @PathParam("account_id") UUID accountId,
            @QueryParam("from") String fromParam,
            @QueryParam("to") String toParam,
            @QueryParam("txn_type") String txnTypeParam) {

        LocalDate from = parseDate(fromParam, "from");
        LocalDate to = parseDate(toParam, "to");
        TxnType txnType = parseTxnType(txnTypeParam);

        List<TransactionResponse> transactions = useCase.listByAccount(accountId, from, to, txnType)
                .stream().map(TransactionResponse::from).toList();
        return Response.ok(new ListTransactionsResponse(transactions)).build();
    }

    @GET
    @Path("/{txn_id}")
    public TransactionResponse getTransaction(
            @PathParam("account_id") UUID accountId,
            @PathParam("txn_id") UUID txnId) {
        return TransactionResponse.from(useCase.getById(txnId));
    }

    private LocalDate parseDate(String value, String paramName) {
        if (value == null) return null;
        try {
            return LocalDate.parse(value);
        } catch (Exception e) {
            throw new BadRequestException("Invalid " + paramName + " date: " + value + " (expected yyyy-MM-dd)");
        }
    }

    private TxnType parseTxnType(String value) {
        if (value == null) return null;
        try {
            return TxnType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid txn_type: " + value + " (expected CREDIT or DEBIT)");
        }
    }
}
