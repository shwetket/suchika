package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.wealth.adapters.services.StatementCsvParser.ParsedRow;
import com.suchika.wealth.domain.TxnType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class StatementCsvParserTest {

    private StatementCsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new StatementCsvParser();
    }

    // ---- HDFC Savings format: split Withdrawal / Deposit columns ----

    @Test
    void parse_hdfcSavings_creditRow() {
        String csv = """
                Date,Narration,Chq./Ref.No.,Value Dt,Withdrawal Amt.,Deposit Amt.,Closing Balance
                20/05/2026,SALARY CREDIT,,20/05/2026,,50000.00,100000.00""";

        List<ParsedRow> rows = parser.parse(csv);

        assertEquals(1, rows.size());
        ParsedRow row = rows.get(0);
        assertEquals(LocalDate.of(2026, Month.MAY, 20), row.date());
        assertEquals(0, new BigDecimal("50000.00").compareTo(row.amount()));
        assertEquals(TxnType.CREDIT, row.txnType());
        assertEquals("SALARY CREDIT", row.description());
    }

    @Test
    void parse_hdfcSavings_debitRow() {
        String csv = """
                Date,Narration,Chq./Ref.No.,Value Dt,Withdrawal Amt.,Deposit Amt.,Closing Balance
                21/05/2026,ATM WITHDRAWAL,,21/05/2026,5000.00,,95000.00""";

        List<ParsedRow> rows = parser.parse(csv);

        assertEquals(1, rows.size());
        assertEquals(TxnType.DEBIT, rows.get(0).txnType());
        assertEquals(0, new BigDecimal("5000.00").compareTo(rows.get(0).amount()));
    }

    @Test
    void parse_hdfcSavings_skipRowsWithNoAmount() {
        String csv = """
                Date,Narration,Chq./Ref.No.,Value Dt,Withdrawal Amt.,Deposit Amt.,Closing Balance
                21/05/2026,OPENING BALANCE,,21/05/2026,,,100000.00
                22/05/2026,UPI PAYMENT,,22/05/2026,1000.00,,99000.00""";

        List<ParsedRow> rows = parser.parse(csv);

        assertEquals(1, rows.size());
        assertEquals("UPI PAYMENT", rows.get(0).description());
    }

    // ---- Credit card format: single Amount column with sign ----

    @Test
    void parse_creditCard_negativeAmount_becomesDebit() {
        String csv = """
                Date,Description,Amount
                15/05/2026,Amazon Purchase,-1234.56""";

        List<ParsedRow> rows = parser.parse(csv);

        assertEquals(1, rows.size());
        ParsedRow row = rows.get(0);
        assertEquals(TxnType.DEBIT, row.txnType());
        assertEquals(0, new BigDecimal("1234.56").compareTo(row.amount()));
    }

    @Test
    void parse_creditCard_positiveAmount_becomesCredit() {
        String csv = """
                Date,Description,Amount
                14/05/2026,Payment Received,5000.00""";

        List<ParsedRow> rows = parser.parse(csv);

        assertEquals(1, rows.size());
        assertEquals(TxnType.CREDIT, rows.get(0).txnType());
    }

    // ---- Multiple rows ----

    @Test
    void parse_multipleRows_allReturned() {
        String csv = """
                Date,Narration,Withdrawal Amt.,Deposit Amt.
                01/06/2026,ROW 1,100.00,
                02/06/2026,ROW 2,,200.00
                03/06/2026,ROW 3,300.00,""";

        List<ParsedRow> rows = parser.parse(csv);

        assertEquals(3, rows.size());
    }

    // ---- Amount normalisation ----

    @Test
    void parse_amountWithCommas_parsedCorrectly() {
        String csv = """
                Date,Description,Amount
                01/06/2026,Big Transfer,"1,50,000.00\"""";

        List<ParsedRow> rows = parser.parse(csv);

        assertEquals(1, rows.size());
        assertEquals(0, new BigDecimal("150000.00").compareTo(rows.get(0).amount()));
    }

    // ---- Date formats ----

    @Test
    void parse_isoDateFormat_parsed() {
        String csv = """
                Date,Description,Amount
                2026-06-01,ISO Date Test,500.00""";

        List<ParsedRow> rows = parser.parse(csv);

        assertEquals(LocalDate.of(2026, Month.JUNE, 1), rows.get(0).date());
    }

    // ---- Error cases ----

    @Test
    void parse_emptyContent_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> parser.parse(""));
    }

    @Test
    void parse_headerOnly_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> parser.parse("Date,Description,Amount"));
    }

    @Test
    void parse_missingDateColumn_throwsBadRequest() {
        String csv = "Description,Amount\n01 June 2026,500.00";
        assertThrows(CsvParseException.class, () -> parser.parse(csv));
    }

    @Test
    void parse_missingAmountColumn_throwsBadRequest() {
        String csv = "Date,Description\n01/06/2026,No Amount Row";
        assertThrows(CsvParseException.class, () -> parser.parse(csv));
    }

    // ---- CsvParseException typed errors ----

    @Test
    void detectColumns_missingDateColumn_throwsCsvParseException() {
        String csv = "Description,Amount\n01 June 2026,500.00";

        CsvParseException ex = assertThrows(CsvParseException.class, () -> parser.parse(csv));

        assertEquals("MISSING_DATE_COLUMN", ex.getErrorType());
        assertTrue(ex.getMissingColumns().contains("date"));
    }

    @Test
    void detectColumns_missingAmountColumn_throwsCsvParseException() {
        String csv = "Date,Description\n01/06/2026,No Amount Row";

        CsvParseException ex = assertThrows(CsvParseException.class, () -> parser.parse(csv));

        assertEquals("MISSING_AMOUNT_COLUMN", ex.getErrorType());
        assertFalse(ex.getMissingColumns().isEmpty());
    }

    // ---- Header-row locator: preamble before the real header row (Kotak-shaped) ----

    @Test
    void parse_preambleBeforeHeader_headerLocatedAndPreambleDiscarded() {
        String csv = """
                Kotak Mahindra Bank
                Statement of Account
                Name: Test User
                Address: 123 Fake Street, Test City
                Account No: XXXXXXXX1234
                IFSC: KKBK0000000
                Period: 01-04-2026 to 30-04-2026
                Sl. No.,Transaction Date,Value Date,Description,Chq /Ref No.,Amount,Dr / Cr,Balance
                1,01-04-2026,01-04-2026,UPI TEST PAYMENT,UPI998877,1000.00,Dr,99000.00""";

        List<ParsedRow> rows = parser.parse(csv);

        assertEquals(1, rows.size());
        assertEquals(LocalDate.of(2026, Month.APRIL, 1), rows.get(0).date());
        assertEquals("UPI TEST PAYMENT", rows.get(0).description());
    }

    @Test
    void parse_footerAfterData_footerLinesSkipped() {
        String csv = """
                Date,Narration,Withdrawal Amt.,Deposit Amt.
                01/06/2026,ROW 1,100.00,
                Closing Balance,,,
                This statement is computer generated and does not require a signature.""";

        List<ParsedRow> rows = parser.parse(csv);

        assertEquals(1, rows.size());
        assertEquals("ROW 1", rows.get(0).description());
    }

    // ---- Indicator-column layout: unsigned Amount + separate Dr / Cr column (Kotak) ----

    @Test
    void parse_indicatorColumn_drRow_becomesDebit() {
        String csv = """
                Date,Description,Chq /Ref No.,Amount,Dr / Cr,Balance
                01-04-2026,ATM WITHDRAWAL,,5000.00,Dr,95000.00""";

        List<ParsedRow> rows = parser.parse(csv);

        assertEquals(1, rows.size());
        assertEquals(TxnType.DEBIT, rows.get(0).txnType());
        assertEquals(0, new BigDecimal("5000.00").compareTo(rows.get(0).amount()));
    }

    @Test
    void parse_indicatorColumn_crRow_becomesCredit() {
        String csv = """
                Date,Description,Chq /Ref No.,Amount,Dr / Cr,Balance
                02-04-2026,SALARY CREDIT,,60000.00,Cr,155000.00""";

        List<ParsedRow> rows = parser.parse(csv);

        assertEquals(1, rows.size());
        assertEquals(TxnType.CREDIT, rows.get(0).txnType());
        assertEquals(0, new BigDecimal("60000.00").compareTo(rows.get(0).amount()));
    }

    @Test
    void parse_indicatorColumn_twoDrCrColumns_amountsOwnIndicatorWinsOverBalanceIndicator() {
        // Kotak's real layout has a Dr / Cr column for Amount AND one for Balance. The
        // Amount-qualifying one appears first (leftmost) in the row and must be the one used.
        String csv = """
                Date,Description,Amount,Dr / Cr,Balance,Dr / Cr
                03-04-2026,UPI PAYMENT,750.00,Dr,94250.00,Cr""";

        List<ParsedRow> rows = parser.parse(csv);

        assertEquals(1, rows.size());
        assertEquals(TxnType.DEBIT, rows.get(0).txnType());
    }

    @Test
    void parse_splitLayoutTakesPrecedenceOverIndicatorAndSignedAmount() {
        // If both split debit/credit columns AND an indicator-shaped Amount/Dr-Cr pair were
        // somehow present, split layout must still win per the documented precedence.
        String csv = """
                Date,Narration,Withdrawal Amt.,Deposit Amt.,Dr / Cr
                01/06/2026,ROW 1,100.00,,Cr""";

        List<ParsedRow> rows = parser.parse(csv);

        assertEquals(1, rows.size());
        assertEquals(TxnType.DEBIT, rows.get(0).txnType());
    }

    // ---- Date-column priority / embedded-timestamp tolerance ----
    // Consolidated (Sonar S5976): these 3 cases only differed in the CSV literal and the
    // expected date — value-date precedence over transaction-date, and date-only extraction
    // when the date column carries an embedded timestamp (dash- or slash-delimited).

    @ParameterizedTest
    @MethodSource("dateExtractionCases")
    void parse_dateExtraction_returnsExpectedDate(String csv, LocalDate expectedDate) {
        List<ParsedRow> rows = parser.parse(csv);

        assertEquals(1, rows.size());
        assertEquals(expectedDate, rows.get(0).date());
    }

    private static Stream<Arguments> dateExtractionCases() {
        return Stream.of(
                Arguments.of("""
                        Transaction Date,Value Date,Description,Amount
                        04-04-2026,06-04-2026,WEEKEND POSTING,1200.00""",
                        LocalDate.of(2026, Month.APRIL, 6)),
                Arguments.of("""
                        Transaction Date,Description,Amount
                        01-04-2026 06:53:55,UPI TEST PAYMENT,1000.00""",
                        LocalDate.of(2026, Month.APRIL, 1)),
                Arguments.of("""
                        Transaction Date,Description,Amount
                        01/04/2026 06:53:55,UPI TEST PAYMENT,1000.00""",
                        LocalDate.of(2026, Month.APRIL, 1))
        );
    }

    // ---- Reference number capture ----

    @Test
    void parse_referenceNumberPresent_captured() {
        String csv = """
                Date,Narration,Chq./Ref.No.,Withdrawal Amt.,Deposit Amt.
                20/05/2026,SALARY CREDIT,UPI/REF/12345,,50000.00""";

        List<ParsedRow> rows = parser.parse(csv);

        assertEquals(1, rows.size());
        assertEquals("UPI/REF/12345", rows.get(0).referenceNumber());
    }

    @Test
    void parse_referenceNumberColumnAbsent_nullNoException() {
        String csv = """
                Date,Description,Amount
                15/05/2026,Amazon Purchase,-1234.56""";

        List<ParsedRow> rows = parser.parse(csv);

        assertEquals(1, rows.size());
        assertNull(rows.get(0).referenceNumber());
    }

    @Test
    void parse_referenceNumberBlank_null() {
        String csv = """
                Date,Narration,Chq./Ref.No.,Withdrawal Amt.,Deposit Amt.
                20/05/2026,SALARY CREDIT,,,50000.00""";

        List<ParsedRow> rows = parser.parse(csv);

        assertEquals(1, rows.size());
        assertNull(rows.get(0).referenceNumber());
    }

    // ---- Amount scale normalisation ----

    @Test
    void parseAmount_variousFormats_normalisedToScale2() {
        String[] amountVariants = {"500", "500.0", "500.000", "500.00"};
        BigDecimal expected = new BigDecimal("500.00");

        for (String variant : amountVariants) {
            String csv = "Date,Description,Amount\n01/06/2026,Test," + variant;
            List<ParsedRow> rows = parser.parse(csv);
            assertEquals(1, rows.size(), "Expected one row for amount: " + variant);
            assertEquals(0, expected.compareTo(rows.get(0).amount()),
                    "Amount scale not normalised for: " + variant);
            assertEquals(2, rows.get(0).amount().scale(),
                    "Scale should be 2 for: " + variant);
        }
    }
}
