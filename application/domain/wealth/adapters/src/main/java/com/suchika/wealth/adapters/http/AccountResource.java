package com.suchika.wealth.adapters.http;

import com.suchika.wealth.adapters.http.dto.AccountBalanceResponse;
import com.suchika.wealth.adapters.http.dto.AccountResponse;
import com.suchika.wealth.adapters.http.dto.AmortizationSummaryResponse;
import com.suchika.wealth.adapters.http.dto.CreateAccountRequest;
import com.suchika.wealth.adapters.http.dto.ListAccountsResponse;
import com.suchika.wealth.adapters.http.dto.UpdateAccountClassificationRequest;
import com.suchika.wealth.adapters.http.dto.UpdateAccountRequest;
import com.suchika.wealth.domain.AccountType;
import com.suchika.wealth.ports.input.AccountUseCase;
import com.suchika.wealth.ports.input.CreateAccountCommand;
import com.suchika.wealth.ports.input.UpdateAccountCommand;
import com.suchika.shared.exception.BadRequestException;
import com.suchika.shared.utils.ResourceUtils;
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
        profileId = ResourceUtils.requireProfileId(profileId);
        AccountType accountType = parseAccountType(accountTypeParam);
        List<AccountResponse> accounts = useCase.listAccounts(profileId, accountType, isActive)
                .stream().map(AccountResponse::from).toList();
        return Response.ok(new ListAccountsResponse(accounts)).build();
    }

    @POST
    public Response createAccount(
            @QueryParam("profile_id") UUID profileId,
            CreateAccountRequest request) {
        profileId = ResourceUtils.requireProfileId(profileId);
        if (request == null) throw new BadRequestException("Request body is required");
        AccountType accountType = parseAccountType(request.accountType);
        if (accountType == null) throw new BadRequestException("account_type is required");
        CreateAccountCommand command = new CreateAccountCommand(
                request.accountName, accountType, request.institutionName, request.currency,
                request.openingBalance, request.creditLimit, request.interestRate, request.emiAmount);
        return Response.status(201).entity(AccountResponse.from(useCase.createAccount(profileId, command))).build();
    }

    @GET
    @Path("/{account_id}")
    public AccountResponse getAccount(
            @PathParam("account_id") UUID accountId,
            @QueryParam("profile_id") UUID profileId) {
        profileId = ResourceUtils.requireProfileId(profileId);
        return AccountResponse.from(useCase.getAccount(accountId, profileId));
    }

    @PATCH
    @Path("/{account_id}")
    public AccountResponse updateAccount(
            @PathParam("account_id") UUID accountId,
            @QueryParam("profile_id") UUID profileId,
            UpdateAccountRequest request) {
        profileId = ResourceUtils.requireProfileId(profileId);
        if (request == null) throw new BadRequestException("Request body is required");
        UpdateAccountCommand command = new UpdateAccountCommand(
                request.accountName, request.openingBalance, request.creditLimit,
                request.interestRate, request.emiAmount, request.active);
        return AccountResponse.from(useCase.updateAccount(accountId, profileId, command));
    }

    @DELETE
    @Path("/{account_id}")
    public Response deactivateAccount(
            @PathParam("account_id") UUID accountId,
            @QueryParam("profile_id") UUID profileId) {
        profileId = ResourceUtils.requireProfileId(profileId);
        useCase.deactivateAccount(accountId, profileId);
        return Response.noContent().build();
    }

    @GET
    @Path("/{account_id}/balance")
    public AccountBalanceResponse getAccountBalance(
            @PathParam("account_id") UUID accountId,
            @QueryParam("profile_id") UUID profileId) {
        profileId = ResourceUtils.requireProfileId(profileId);
        return AccountBalanceResponse.from(useCase.getAccountBalance(accountId, profileId));
    }

    @PATCH
    @Path("/{account_id}/classification")
    public AccountResponse updateAccountClassification(
            @PathParam("account_id") UUID accountId,
            @QueryParam("profile_id") UUID profileId,
            UpdateAccountClassificationRequest request) {
        profileId = ResourceUtils.requireProfileId(profileId);
        if (request == null) throw new BadRequestException("Request body is required");
        return AccountResponse.from(useCase.updateAccountClassification(accountId, profileId,
                new com.suchika.wealth.ports.input.UpdateAccountClassificationCommand(
                        request.category, request.liquidityTier, request.purposeTag, request.jointOwners,
                        request.loanOriginalPrincipal, request.loanStartDate, request.loanTenureMonths,
                        request.linkedOffsetAccountId)));
    }

    @GET
    @Path("/{account_id}/amortization")
    public AmortizationSummaryResponse getAmortization(
            @PathParam("account_id") UUID accountId,
            @QueryParam("profile_id") UUID profileId) {
        profileId = ResourceUtils.requireProfileId(profileId);
        return AmortizationSummaryResponse.from(useCase.getAmortization(accountId, profileId));
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
