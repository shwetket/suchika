package com.suchika.wealth.adapters.http;

import com.suchika.wealth.adapters.http.dto.AccountBalanceResponse;
import com.suchika.wealth.adapters.http.dto.AccountResponse;
import com.suchika.wealth.adapters.http.dto.CreateAccountRequest;
import com.suchika.wealth.adapters.http.dto.ListAccountsResponse;
import com.suchika.wealth.adapters.http.dto.UpdateAccountClassificationRequest;
import com.suchika.wealth.adapters.http.dto.UpdateAccountRequest;
import com.suchika.wealth.domain.AccountType;
import com.suchika.wealth.ports.input.AccountUseCase;
import com.suchika.wealth.ports.input.CreateAccountCommand;
import com.suchika.shared.exception.BadRequestException;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.UUID;

@Path("/v1/accounts")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AccountResource {

    private final AccountUseCase useCase;

    public AccountResource(AccountUseCase useCase) {
        this.useCase = useCase;
    }

    @GET
    public Response listAccounts(
            @QueryParam("account_type") String accountTypeParam,
            @QueryParam("is_active") Boolean isActive,
            @QueryParam("profile_id") UUID profileId) {
        AccountType accountType = parseAccountType(accountTypeParam);
        List<AccountResponse> accounts = useCase.listAccounts(profileId, accountType, isActive)
                .stream().map(AccountResponse::from).toList();
        return Response.ok(new ListAccountsResponse(accounts)).build();
    }

    @POST
    public Response createAccount(
            @QueryParam("profile_id") UUID profileId,
            CreateAccountRequest request) {
        if (request == null) throw new BadRequestException("Request body is required");
        AccountType accountType = parseAccountType(request.accountType);
        if (accountType == null) throw new BadRequestException("account_type is required");
        CreateAccountCommand command = new CreateAccountCommand(
                request.accountName, accountType, request.institutionName,
                request.openingBalance, request.creditLimit, request.interestRate, request.emiAmount);
        return Response.status(201).entity(AccountResponse.from(useCase.createAccount(profileId, command))).build();
    }

    @GET
    @Path("/{account_id}")
    public AccountResponse getAccount(@PathParam("account_id") UUID accountId) {
        return AccountResponse.from(useCase.getAccount(accountId));
    }

    @PATCH
    @Path("/{account_id}")
    public AccountResponse updateAccount(@PathParam("account_id") UUID accountId, UpdateAccountRequest request) {
        if (request == null) throw new BadRequestException("Request body is required");
        return AccountResponse.from(
                useCase.updateAccount(accountId, request.accountName, request.openingBalance,
                        request.creditLimit, request.interestRate, request.emiAmount, request.active));
    }

    @DELETE
    @Path("/{account_id}")
    public Response deactivateAccount(@PathParam("account_id") UUID accountId) {
        useCase.deactivateAccount(accountId);
        return Response.noContent().build();
    }

    @GET
    @Path("/{account_id}/balance")
    public AccountBalanceResponse getAccountBalance(
            @PathParam("account_id") UUID accountId,
            @QueryParam("profile_id") UUID profileId) {
        return AccountBalanceResponse.from(useCase.getAccountBalance(accountId, profileId));
    }

    @PATCH
    @Path("/{account_id}/classification")
    public AccountResponse updateAccountClassification(
            @PathParam("account_id") UUID accountId,
            UpdateAccountClassificationRequest request) {
        if (request == null) throw new BadRequestException("Request body is required");
        return AccountResponse.from(useCase.updateAccountClassification(
                accountId, request.category, request.liquidityTier, request.purposeTag, request.jointOwners));
    }

    private AccountType parseAccountType(String value) {
        if (value == null) return null;
        try {
            return AccountType.valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid account_type: " + value);
        }
    }
}
