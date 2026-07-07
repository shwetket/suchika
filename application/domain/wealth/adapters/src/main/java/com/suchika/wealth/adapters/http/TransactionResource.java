package com.suchika.wealth.adapters.http;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.wealth.adapters.http.dto.BulkUpdateTransactionCategoryRequest;
import com.suchika.wealth.adapters.http.dto.CreateTransactionRequest;
import com.suchika.wealth.adapters.http.dto.ListTransactionsResponse;
import com.suchika.wealth.adapters.http.dto.TransactionResponse;
import com.suchika.wealth.adapters.http.dto.UpdateTransactionCategoryRequest;
import com.suchika.wealth.domain.ExpenseCategory;
import com.suchika.wealth.domain.TxnType;
import com.suchika.wealth.ports.input.CreateTransactionCommand;
import com.suchika.wealth.ports.input.PagedTransactions;
import com.suchika.wealth.ports.input.TransactionUseCase;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.suchika.shared.utils.ResourceUtils;

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
            @QueryParam("profile_id") UUID profileId,
            @QueryParam("from") String fromParam,
            @QueryParam("to") String toParam,
            @QueryParam("txn_type") String txnTypeParam,
            @QueryParam("page") Integer pageParam,
            @QueryParam("size") Integer sizeParam) {

        LocalDate from = ResourceUtils.parseDate(fromParam, "from");
        LocalDate to = ResourceUtils.parseDate(toParam, "to");
        TxnType txnType = parseTxnType(txnTypeParam);
        int page = ResourceUtils.parsePage(pageParam);
        int size = ResourceUtils.parseSize(sizeParam);

        PagedTransactions result = useCase.listByAccountPaginated(accountId, profileId, from, to, txnType, page, size);
        List<TransactionResponse> transactions = result.transactions().stream().map(TransactionResponse::from).toList();
        return Response.ok(new ListTransactionsResponse(transactions, result.totalCount(), page, size)).build();
    }

    @POST
    public Response createTransaction(
            @PathParam("account_id") UUID accountId,
            @QueryParam("profile_id") UUID profileId,
            CreateTransactionRequest request) {
        if (request == null) throw new BadRequestException("Request body is required");
        TxnType txnType = parseTxnType(request.txnType);
        if (txnType == null) throw new BadRequestException("txn_type is required");
        LocalDate txnDate = ResourceUtils.parseDate(request.txnDate, "txn_date");
        if (txnDate == null) throw new BadRequestException("txn_date is required");
        CreateTransactionCommand command = new CreateTransactionCommand(
                txnDate, request.amount, txnType, request.description);
        return Response.status(201)
                .entity(TransactionResponse.from(useCase.create(accountId, profileId, command)))
                .build();
    }

    @GET
    @Path("/{txn_id}")
    public TransactionResponse getTransaction(
            @PathParam("account_id") UUID accountId,
            @PathParam("txn_id") UUID txnId) {
        return TransactionResponse.from(useCase.getById(txnId));
    }

    @PATCH
    @Path("/{txn_id}/category")
    public TransactionResponse updateCategory(
            @PathParam("account_id") UUID accountId,
            @PathParam("txn_id") UUID txnId,
            UpdateTransactionCategoryRequest request) {
        if (request == null) throw new BadRequestException("Request body is required");
        return TransactionResponse.from(useCase.updateCategory(txnId, parseCategory(request.category)));
    }

    @PATCH
    @Path("/category")
    public Response bulkUpdateCategory(
            @PathParam("account_id") UUID accountId,
            BulkUpdateTransactionCategoryRequest request) {
        if (request == null) throw new BadRequestException("Request body is required");
        ExpenseCategory category = parseCategory(request.category);
        List<TransactionResponse> updated = useCase.bulkUpdateCategory(request.transactionIds, category)
                .stream().map(TransactionResponse::from).toList();
        return Response.ok(new ListTransactionsResponse(updated)).build();
    }

    private ExpenseCategory parseCategory(String value) {
        if (value == null) throw new BadRequestException("category is required");
        try {
            return ExpenseCategory.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid category: " + value);
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
