package com.suchika.wealth.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link AmortizationCalculator}.
 *
 * <p>The calculator takes {@code asOfDate} as an explicit parameter (rather than reading
 * {@code LocalDate.now()} internally), so every test here pins a fixed date instead of relying
 * on whatever day the test happens to run — the calculator is now fully deterministic.
 */
class AmortizationCalculatorTest {

    private static final BigDecimal ZERO_RATE = BigDecimal.ZERO;

    @Test
    void compute_asOfStartDate_zeroElapsedMonths_outstandingEqualsPrincipal() {
        BigDecimal principal = new BigDecimal("120000");
        LocalDate startDate = LocalDate.of(2020, Month.JANUARY, 1);
        LocalDate asOfDate = startDate;

        AmortizationSummary summary =
                AmortizationCalculator.compute(principal, ZERO_RATE, 12, startDate, asOfDate);

        assertEquals(0, summary.getMonthlyEmi().subtract(new BigDecimal("10000.00")).signum());
        assertEquals(12, summary.getRemainingMonths());
        assertEquals(0, summary.getOutstandingBalance().compareTo(new BigDecimal("120000.00")));
        assertEquals(0, summary.getPrincipalPaid().compareTo(BigDecimal.ZERO.setScale(2)));
        assertEquals(0, summary.getInterestPaid().compareTo(BigDecimal.ZERO.setScale(2)));
    }

    @Test
    void compute_sixMonthsElapsed_halfPrincipalPaidOff() {
        BigDecimal principal = new BigDecimal("120000");
        LocalDate startDate = LocalDate.of(2020, Month.JANUARY, 1);
        LocalDate asOfDate = LocalDate.of(2020, Month.JULY, 1);

        AmortizationSummary summary =
                AmortizationCalculator.compute(principal, ZERO_RATE, 12, startDate, asOfDate);

        assertEquals(6, summary.getRemainingMonths());
        assertEquals(0, summary.getOutstandingBalance().compareTo(new BigDecimal("60000.00")));
        assertEquals(0, summary.getPrincipalPaid().compareTo(new BigDecimal("60000.00")));
        assertEquals(0, summary.getTotalInterestRemaining().compareTo(BigDecimal.ZERO.setScale(2)));
    }

    @Test
    void compute_asOfDateAfterTenureEnds_loanFullyPaidOff() {
        BigDecimal principal = new BigDecimal("120000");
        LocalDate startDate = LocalDate.of(2020, Month.JANUARY, 1);
        LocalDate asOfDate = LocalDate.of(2021, Month.JUNE, 1);

        AmortizationSummary summary =
                AmortizationCalculator.compute(principal, ZERO_RATE, 12, startDate, asOfDate);

        assertEquals(0, summary.getRemainingMonths());
        assertEquals(0, summary.getOutstandingBalance().compareTo(BigDecimal.ZERO.setScale(2)));
        assertEquals(0, summary.getPrincipalPaid().compareTo(principal.setScale(2)));
        assertEquals(0, summary.getTotalInterestRemaining().compareTo(BigDecimal.ZERO.setScale(2)));
    }

    @Test
    void compute_asOfDateBeforeStartDate_elapsedMonthsClampedToZero() {
        BigDecimal principal = new BigDecimal("120000");
        LocalDate startDate = LocalDate.of(2026, Month.JANUARY, 1);
        LocalDate asOfDate = LocalDate.of(2025, Month.JANUARY, 1);

        AmortizationSummary summary =
                AmortizationCalculator.compute(principal, ZERO_RATE, 12, startDate, asOfDate);

        assertEquals(12, summary.getRemainingMonths());
        assertEquals(0, summary.getOutstandingBalance().compareTo(new BigDecimal("120000.00")));
    }

    @Test
    void compute_nonZeroRate_exactRemainingMonthsForFixedAsOfDate() {
        // With a real system clock this remainingMonths value would only ever be correct on
        // one specific calendar day; pinning asOfDate makes the exact expectation reliable.
        BigDecimal principal = new BigDecimal("100000");
        BigDecimal annualRate = new BigDecimal("12");
        LocalDate startDate = LocalDate.of(2024, Month.JANUARY, 1);
        LocalDate asOfDate = LocalDate.of(2024, Month.OCTOBER, 1);

        AmortizationSummary summary =
                AmortizationCalculator.compute(principal, annualRate, 12, startDate, asOfDate);

        assertEquals(3, summary.getRemainingMonths());
        assertTrue(summary.getMonthlyEmi().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(summary.getOutstandingBalance().compareTo(principal) < 0);
        assertTrue(summary.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void compute_calledTwiceWithSameFixedAsOfDate_isDeterministic() {
        BigDecimal principal = new BigDecimal("500000");
        BigDecimal annualRate = new BigDecimal("8.75");
        LocalDate startDate = LocalDate.of(2022, Month.MARCH, 15);
        LocalDate asOfDate = LocalDate.of(2026, Month.JULY, 8);

        AmortizationSummary first =
                AmortizationCalculator.compute(principal, annualRate, 60, startDate, asOfDate);
        AmortizationSummary second =
                AmortizationCalculator.compute(principal, annualRate, 60, startDate, asOfDate);

        assertEquals(first.getRemainingMonths(), second.getRemainingMonths());
        assertEquals(0, first.getMonthlyEmi().compareTo(second.getMonthlyEmi()));
        assertEquals(0, first.getOutstandingBalance().compareTo(second.getOutstandingBalance()));
        assertEquals(0, first.getTotalInterestRemaining().compareTo(second.getTotalInterestRemaining()));
        assertEquals(0, first.getPrincipalPaid().compareTo(second.getPrincipalPaid()));
        assertEquals(0, first.getInterestPaid().compareTo(second.getInterestPaid()));
    }
}
