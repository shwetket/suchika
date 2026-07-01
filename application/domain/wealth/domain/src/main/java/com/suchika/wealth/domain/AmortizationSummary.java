package com.suchika.wealth.domain;

import java.math.BigDecimal;

/**
 * Computed amortization snapshot for a loan account.
 * Epic 8 Phase 3 — produced by {@link AmortizationCalculator}.
 * Pure value class — no framework dependencies.
 */
public final class AmortizationSummary {

    private final BigDecimal monthlyEmi;
    private final BigDecimal outstandingBalance;
    private final int remainingMonths;
    private final BigDecimal totalInterestRemaining;
    private final BigDecimal principalPaid;
    private final BigDecimal interestPaid;

    private AmortizationSummary(Builder builder) {
        this.monthlyEmi = builder.monthlyEmi;
        this.outstandingBalance = builder.outstandingBalance;
        this.remainingMonths = builder.remainingMonths;
        this.totalInterestRemaining = builder.totalInterestRemaining;
        this.principalPaid = builder.principalPaid;
        this.interestPaid = builder.interestPaid;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private BigDecimal monthlyEmi;
        private BigDecimal outstandingBalance;
        private int remainingMonths;
        private BigDecimal totalInterestRemaining;
        private BigDecimal principalPaid;
        private BigDecimal interestPaid;

        public Builder monthlyEmi(BigDecimal monthlyEmi) {
            this.monthlyEmi = monthlyEmi;
            return this;
        }

        public Builder outstandingBalance(BigDecimal outstandingBalance) {
            this.outstandingBalance = outstandingBalance;
            return this;
        }

        public Builder remainingMonths(int remainingMonths) {
            this.remainingMonths = remainingMonths;
            return this;
        }

        public Builder totalInterestRemaining(BigDecimal totalInterestRemaining) {
            this.totalInterestRemaining = totalInterestRemaining;
            return this;
        }

        public Builder principalPaid(BigDecimal principalPaid) {
            this.principalPaid = principalPaid;
            return this;
        }

        public Builder interestPaid(BigDecimal interestPaid) {
            this.interestPaid = interestPaid;
            return this;
        }

        public AmortizationSummary build() {
            return new AmortizationSummary(this);
        }
    }

    public BigDecimal getMonthlyEmi() {
        return monthlyEmi;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public int getRemainingMonths() {
        return remainingMonths;
    }

    public BigDecimal getTotalInterestRemaining() {
        return totalInterestRemaining;
    }

    public BigDecimal getPrincipalPaid() {
        return principalPaid;
    }

    public BigDecimal getInterestPaid() {
        return interestPaid;
    }
}
