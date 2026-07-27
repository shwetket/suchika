package com.suchika.wealth.domain;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Pure stateless calculator for loan amortization.
 * Epic 8 Phase 3 — no framework dependencies.
 *
 * <p>EMI formula: EMI = P * r * (1+r)^n / ((1+r)^n - 1)
 * where P = principal, r = monthly rate (annualRate / 12 / 100), n = tenure in months.
 *
 * <p>Outstanding balance at elapsed month e:
 * outstanding = EMI * ((1+r)^remaining - 1) / (r * (1+r)^remaining)
 * where remaining = n - e.
 *
 * <p>All intermediate calculations use {@link MathContext#DECIMAL128}.
 * Final amounts are rounded to 2 decimal places with {@link RoundingMode#HALF_UP}.
 */
public final class AmortizationCalculator {

    private static final MathContext MC = MathContext.DECIMAL128;
    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal TWELVE = new BigDecimal("12");

    private AmortizationCalculator() {
        // utility class — no instances
    }

    /**
     * Computes the amortization summary as of the given date.
     *
     * @param principal        original loan principal (positive)
     * @param annualRatePercent annual interest rate in percent (e.g. 8.75 for 8.75%). May be zero.
     * @param tenureMonths     original loan tenure in months (positive)
     * @param startDate        EMI start date (first payment date / disbursement date)
     * @param asOfDate         the date to compute the summary as of (caller-supplied "today")
     * @return computed summary; all monetary amounts rounded to 2 dp
     */
    public static AmortizationSummary compute(
            BigDecimal principal,
            BigDecimal annualRatePercent,
            int tenureMonths,
            LocalDate startDate,
            LocalDate asOfDate) {

        int elapsedMonths = (int) ChronoUnit.MONTHS.between(startDate, asOfDate);
        if (elapsedMonths < 0) {
            elapsedMonths = 0;
        }

        int remainingMonths = tenureMonths - elapsedMonths;
        if (remainingMonths <= 0) {
            // Loan fully paid off
            return AmortizationSummary.builder()
                    .monthlyEmi(computeEmi(principal, annualRatePercent, tenureMonths))
                    .outstandingBalance(BigDecimal.ZERO.setScale(SCALE, ROUNDING))
                    .remainingMonths(0)
                    .totalInterestRemaining(BigDecimal.ZERO.setScale(SCALE, ROUNDING))
                    .principalPaid(principal.setScale(SCALE, ROUNDING))
                    .interestPaid(computeTotalInterestPaid(principal, annualRatePercent, tenureMonths))
                    .build();
        }

        BigDecimal emi = computeEmi(principal, annualRatePercent, tenureMonths);
        BigDecimal outstanding = computeOutstanding(emi, annualRatePercent, remainingMonths);
        BigDecimal totalInterestRemaining = emi.multiply(new BigDecimal(remainingMonths), MC)
                .subtract(outstanding, MC)
                .setScale(SCALE, ROUNDING);
        if (totalInterestRemaining.compareTo(BigDecimal.ZERO) < 0) {
            totalInterestRemaining = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }

        BigDecimal principalPaid = principal.subtract(outstanding, MC).setScale(SCALE, ROUNDING);
        if (principalPaid.compareTo(BigDecimal.ZERO) < 0) {
            principalPaid = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }

        BigDecimal totalPaymentsMade = emi.multiply(new BigDecimal(elapsedMonths), MC);
        BigDecimal interestPaid = totalPaymentsMade.subtract(principalPaid, MC).setScale(SCALE, ROUNDING);
        if (interestPaid.compareTo(BigDecimal.ZERO) < 0) {
            interestPaid = BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }

        return AmortizationSummary.builder()
                .monthlyEmi(emi)
                .outstandingBalance(outstanding)
                .remainingMonths(remainingMonths)
                .totalInterestRemaining(totalInterestRemaining)
                .principalPaid(principalPaid)
                .interestPaid(interestPaid)
                .build();
    }

    // EMI = P * r * (1+r)^n / ((1+r)^n - 1)
    // For zero rate: EMI = P / n
    private static BigDecimal computeEmi(BigDecimal principal, BigDecimal annualRatePercent, int tenureMonths) {
        if (annualRatePercent == null || annualRatePercent.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(new BigDecimal(tenureMonths), MC).setScale(SCALE, ROUNDING);
        }
        BigDecimal monthlyRate = annualRatePercent.divide(TWELVE.multiply(HUNDRED, MC), MC);
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate, MC);
        BigDecimal onePlusRPowN = onePlusR.pow(tenureMonths, MC);
        BigDecimal numerator = principal.multiply(monthlyRate, MC).multiply(onePlusRPowN, MC);
        BigDecimal denominator = onePlusRPowN.subtract(BigDecimal.ONE, MC);
        return numerator.divide(denominator, MC).setScale(SCALE, ROUNDING);
    }

    // outstanding = EMI * ((1+r)^remaining - 1) / (r * (1+r)^remaining)
    // For zero rate: outstanding = EMI * remaining
    private static BigDecimal computeOutstanding(BigDecimal emi, BigDecimal annualRatePercent, int remainingMonths) {
        if (annualRatePercent == null || annualRatePercent.compareTo(BigDecimal.ZERO) == 0) {
            return emi.multiply(new BigDecimal(remainingMonths), MC).setScale(SCALE, ROUNDING);
        }
        BigDecimal monthlyRate = annualRatePercent.divide(TWELVE.multiply(HUNDRED, MC), MC);
        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate, MC);
        BigDecimal onePlusRPowRem = onePlusR.pow(remainingMonths, MC);
        BigDecimal numerator = emi.multiply(onePlusRPowRem.subtract(BigDecimal.ONE, MC), MC);
        BigDecimal denominator = monthlyRate.multiply(onePlusRPowRem, MC);
        return numerator.divide(denominator, MC).setScale(SCALE, ROUNDING);
    }

    // Total interest = (EMI * n) - principal
    private static BigDecimal computeTotalInterestPaid(BigDecimal principal, BigDecimal annualRatePercent, int tenureMonths) {
        BigDecimal emi = computeEmi(principal, annualRatePercent, tenureMonths);
        BigDecimal total = emi.multiply(new BigDecimal(tenureMonths), MC);
        BigDecimal interest = total.subtract(principal, MC).setScale(SCALE, ROUNDING);
        if (interest.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(SCALE, ROUNDING);
        }
        return interest;
    }
}
