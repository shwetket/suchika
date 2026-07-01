package com.suchika.wealth.adapters.http.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.suchika.wealth.domain.AmortizationSummary;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;

/**
 * HTTP response DTO for the amortization summary endpoint.
 * Epic 8 Phase 3.
 */
@RegisterForReflection
public class AmortizationSummaryResponse {

    @JsonProperty("monthly_emi")
    public BigDecimal monthlyEmi;

    @JsonProperty("outstanding_balance")
    public BigDecimal outstandingBalance;

    @JsonProperty("remaining_months")
    public int remainingMonths;

    @JsonProperty("total_interest_remaining")
    public BigDecimal totalInterestRemaining;

    @JsonProperty("principal_paid")
    public BigDecimal principalPaid;

    @JsonProperty("interest_paid")
    public BigDecimal interestPaid;

    public static AmortizationSummaryResponse from(AmortizationSummary summary) {
        AmortizationSummaryResponse r = new AmortizationSummaryResponse();
        r.monthlyEmi = summary.getMonthlyEmi();
        r.outstandingBalance = summary.getOutstandingBalance();
        r.remainingMonths = summary.getRemainingMonths();
        r.totalInterestRemaining = summary.getTotalInterestRemaining();
        r.principalPaid = summary.getPrincipalPaid();
        r.interestPaid = summary.getInterestPaid();
        return r;
    }
}
