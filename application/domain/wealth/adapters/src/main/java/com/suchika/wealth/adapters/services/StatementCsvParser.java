package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.BadRequestException;
import com.suchika.wealth.domain.TxnType;
import jakarta.enterprise.context.ApplicationScoped;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Parses bank CSV statements into structured ParsedRow records.
 *
 * <p>Supports three column layouts:
 * <ol>
 *   <li>Split debit/credit: separate Withdrawal/Deposit or Debit/Credit columns (HDFC savings).
 *   <li>Amount + indicator: one unsigned Amount column plus a separate Dr / Cr indicator column
 *       (Kotak savings).
 *   <li>Signed single amount: one Amount column; negative values → DEBIT, positive → CREDIT
 *       (credit card style exports).
 * </ol>
 * Layout precedence when more than one is detectable: split debit/credit &gt; indicator &gt;
 * signed single amount.
 *
 * <p>All detected headers are matched case-insensitively after trimming.
 * Amount values are normalised to absolute (positive) before storage; direction is in TxnType.
 */
@ApplicationScoped
public class StatementCsvParser {

    public record ParsedRow(LocalDate date, BigDecimal amount, TxnType txnType, String description,
                             String referenceNumber) {
        public ParsedRow withDescription(String newDescription) {
            return new ParsedRow(date, amount, txnType, newDescription, referenceNumber);
        }
    }

    /**
     * Ordered by preference, not by position. Kotak statements have both "Transaction Date"
     * (with an embedded timestamp) and "Value Date" (clean date) — they can legitimately differ
     * on weekend/holiday postings, and the bank's own statement footer recommends Value Date for
     * reconciliation. So date-column detection iterates this list in order and returns the first
     * candidate that matches any header, rather than the leftmost-header-wins rule used for the
     * other columns below.
     */
    private static final List<String> DATE_HEADERS = List.of(
            "value date", "value dt", "transaction date", "txn date", "date", "posting date"
    );
    private static final List<String> DESCRIPTION_HEADERS = List.of(
            "narration", "description", "particulars", "memo", "transaction details", "details"
    );
    private static final List<String> DEBIT_HEADERS = List.of(
            "withdrawal amt.", "withdrawal", "debit", "debit amount", "dr", "withdrawal amount"
    );
    private static final List<String> CREDIT_HEADERS = List.of(
            "deposit amt.", "deposit", "credit", "credit amount", "cr", "deposit amount"
    );
    private static final List<String> AMOUNT_HEADERS = List.of(
            "amount", "txn amount", "transaction amount", "net amount"
    );
    /**
     * Kotak's header row has TWO "Dr / Cr" columns — one qualifying Amount, one qualifying
     * Balance. Leftmost-match-wins (the same rule used for every other non-date column) happens
     * to bind correctly here because Amount's own indicator column appears first in the row, but
     * that's a real, position-dependent assumption about the Kotak layout — not something the
     * code alone makes obvious.
     */
    private static final List<String> INDICATOR_HEADERS = List.of("dr / cr", "dr/cr", "cr/dr");
    private static final List<String> REFERENCE_HEADERS = List.of(
            "chq /ref no.", "chq./ref.no.", "cheque no", "ref no", "reference number"
    );
    private static final List<String> DEBIT_INDICATOR_TOKENS = List.of("dr", "debit");
    private static final List<String> CREDIT_INDICATOR_TOKENS = List.of("cr", "credit");

    private static final int MAX_HEADER_SCAN_LINES = 50;

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd MMM yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy"),
            // Defensive tolerance for embedded timestamps (e.g. Kotak's "Transaction Date" column,
            // "01-04-2026 06:53:55"). LocalDate.parse ignores the resolved time-of-day fields.
            DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
    );

    public List<ParsedRow> parse(String csvContent) {
        if (csvContent == null || csvContent.isBlank()) {
            throw new BadRequestException("CSV content is empty");
        }

        List<String> lines = Arrays.stream(csvContent.split("\\r?\\n"))
                .map(String::trim)
                .filter(l -> !l.isBlank())
                .toList();

        if (lines.size() < 2) {
            throw new BadRequestException("CSV must have a header row and at least one data row");
        }

        HeaderMatch headerMatch = locateHeaderRow(lines);
        ColumnMap cols = headerMatch.columns();

        List<ParsedRow> rows = new ArrayList<>();
        for (int i = headerMatch.rowIndex() + 1; i < lines.size(); i++) {
            String[] fields = parseCsvLine(lines.get(i));
            if (fields.length <= Math.max(cols.dateCol(), cols.descriptionCol())) {
                continue;
            }
            ParsedRow row = parseRow(fields, cols);
            if (row != null) {
                rows.add(row);
            }
        }

        if (rows.isEmpty()) {
            throw new BadRequestException("No valid transactions found in CSV");
        }
        return rows;
    }

    private record HeaderMatch(int rowIndex, ColumnMap columns) {}

    /**
     * Scans lines in order (capped at {@link #MAX_HEADER_SCAN_LINES}) for the first line that
     * parses as a valid header row. Real bank exports (e.g. Kotak) prepend account-metadata
     * preamble — name, address, IFSC/MICR, statement period, etc. — before the actual header
     * row; those preamble lines are discarded here. Footer lines after the data (e.g. Kotak's
     * "Closing Balance" / disclaimer block) don't need special handling — they're already
     * naturally skipped by the row-length and null-date/description checks in {@link #parse}.
     *
     * <p>If no line in the scanned range is a valid header (e.g. the CSV genuinely has no usable
     * header), line 0 is re-attempted so the thrown {@link CsvParseException} carries the same
     * error it always has — preserving prior behaviour for malformed input.
     */
    private HeaderMatch locateHeaderRow(List<String> lines) {
        int scanLimit = Math.min(lines.size(), MAX_HEADER_SCAN_LINES);
        for (int i = 0; i < scanLimit; i++) {
            try {
                ColumnMap cols = detectColumns(parseCsvLine(lines.get(i)));
                return new HeaderMatch(i, cols);
            } catch (CsvParseException ignored) {
                // not a header row (preamble line) — keep scanning
            }
        }
        return new HeaderMatch(0, detectColumns(parseCsvLine(lines.get(0))));
    }

    private record ColumnMap(int dateCol, int descriptionCol, Integer debitCol, Integer creditCol,
                              Integer amountCol, Integer indicatorCol, Integer referenceCol) {
        boolean isSplitLayout() {
            return debitCol != null || creditCol != null;
        }

        boolean isIndicatorLayout() {
            return !isSplitLayout() && amountCol != null && indicatorCol != null;
        }
    }

    private ColumnMap detectColumns(String[] headers) {
        Integer dateCol = findColumnByPriority(headers, DATE_HEADERS);
        Integer descriptionCol = findColumn(headers, DESCRIPTION_HEADERS);
        Integer debitCol = findColumn(headers, DEBIT_HEADERS);
        Integer creditCol = findColumn(headers, CREDIT_HEADERS);
        Integer amountCol = findColumn(headers, AMOUNT_HEADERS);
        Integer indicatorCol = findColumn(headers, INDICATOR_HEADERS);
        Integer referenceCol = findColumn(headers, REFERENCE_HEADERS);

        if (dateCol == null) {
            throw CsvParseException.missingDateColumn(headers);
        }
        if (descriptionCol == null) {
            throw CsvParseException.missingRequiredColumn("description", headers);
        }
        if (debitCol == null && creditCol == null && amountCol == null) {
            throw CsvParseException.missingAmountColumn(headers);
        }

        return new ColumnMap(dateCol, descriptionCol, debitCol, creditCol, amountCol, indicatorCol, referenceCol);
    }

    private ParsedRow parseRow(String[] fields, ColumnMap cols) {
        String rawDate = safeGet(fields, cols.dateCol());
        String rawDescription = safeGet(fields, cols.descriptionCol());

        if (rawDate == null || rawDate.isBlank() || rawDescription == null || rawDescription.isBlank()) {
            return null;
        }

        LocalDate date = parseDate(rawDate);
        if (date == null) return null;

        String description = rawDescription.trim();
        String referenceNumber = extractReferenceNumber(fields, cols);

        if (cols.isSplitLayout()) {
            return parseSplitAmountRow(fields, cols, date, description, referenceNumber);
        } else if (cols.isIndicatorLayout()) {
            return parseIndicatorAmountRow(fields, cols, date, description, referenceNumber);
        } else {
            return parseSingleAmountRow(fields, cols, date, description, referenceNumber);
        }
    }

    private String extractReferenceNumber(String[] fields, ColumnMap cols) {
        if (cols.referenceCol() == null) return null;
        String raw = safeGet(fields, cols.referenceCol());
        if (raw == null || raw.isBlank()) return null;
        return raw.trim();
    }

    private ParsedRow parseSplitAmountRow(String[] fields, ColumnMap cols, LocalDate date, String description,
                                           String referenceNumber) {
        String rawDebit = cols.debitCol() != null ? safeGet(fields, cols.debitCol()) : null;
        String rawCredit = cols.creditCol() != null ? safeGet(fields, cols.creditCol()) : null;

        BigDecimal debit = parseAmount(rawDebit);
        BigDecimal credit = parseAmount(rawCredit);

        if (debit != null && debit.compareTo(BigDecimal.ZERO) > 0) {
            return new ParsedRow(date, debit.abs(), TxnType.DEBIT, description, referenceNumber);
        }
        if (credit != null && credit.compareTo(BigDecimal.ZERO) > 0) {
            return new ParsedRow(date, credit.abs(), TxnType.CREDIT, description, referenceNumber);
        }
        return null;
    }

    private ParsedRow parseIndicatorAmountRow(String[] fields, ColumnMap cols, LocalDate date, String description,
                                               String referenceNumber) {
        String rawAmount = safeGet(fields, cols.amountCol());
        BigDecimal amount = parseAmount(rawAmount);
        if (amount == null) return null;

        String rawIndicator = safeGet(fields, cols.indicatorCol());
        if (rawIndicator == null || rawIndicator.isBlank()) return null;
        String indicator = rawIndicator.trim().toLowerCase();

        if (DEBIT_INDICATOR_TOKENS.contains(indicator)) {
            return new ParsedRow(date, amount.abs(), TxnType.DEBIT, description, referenceNumber);
        }
        if (CREDIT_INDICATOR_TOKENS.contains(indicator)) {
            return new ParsedRow(date, amount.abs(), TxnType.CREDIT, description, referenceNumber);
        }
        return null;
    }

    private ParsedRow parseSingleAmountRow(String[] fields, ColumnMap cols, LocalDate date, String description,
                                            String referenceNumber) {
        if (cols.amountCol() == null) return null;
        String rawAmount = safeGet(fields, cols.amountCol());
        BigDecimal amount = parseAmount(rawAmount);
        if (amount == null) return null;

        TxnType txnType = amount.compareTo(BigDecimal.ZERO) < 0 ? TxnType.DEBIT : TxnType.CREDIT;
        return new ParsedRow(date, amount.abs(), txnType, description, referenceNumber);
    }

    private Integer findColumn(String[] headers, List<String> candidates) {
        for (int i = 0; i < headers.length; i++) {
            String h = headers[i].trim().toLowerCase();
            if (candidates.contains(h)) {
                return i;
            }
        }
        return null;
    }

    /**
     * Iterates candidates in priority order (outer loop) rather than by header position, so a
     * higher-priority candidate found anywhere in the row wins over a lower-priority one that
     * happens to sit further left. Used only for date-column detection — see {@link #DATE_HEADERS}.
     */
    private Integer findColumnByPriority(String[] headers, List<String> candidatesInPriorityOrder) {
        for (String candidate : candidatesInPriorityOrder) {
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].trim().toLowerCase().equals(candidate)) {
                    return i;
                }
            }
        }
        return null;
    }

    private String safeGet(String[] fields, int index) {
        if (index < 0 || index >= fields.length) return null;
        return fields[index].trim();
    }

    private LocalDate parseDate(String raw) {
        String cleaned = raw.trim();
        for (DateTimeFormatter fmt : DATE_FORMATS) {
            try {
                return LocalDate.parse(cleaned, fmt);
            } catch (DateTimeParseException ignored) {
                // not parseable with this format — try next
            }
        }
        return null;
    }

    private BigDecimal parseAmount(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String cleaned = raw.trim()
                .replace(",", "")
                .replace("\"", "")
                .replace(" ", "");
        if (cleaned.isEmpty()) return null;
        try {
            return new BigDecimal(cleaned).setScale(2, RoundingMode.HALF_UP);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Simple CSV line parser that handles quoted fields.
     * Uses a while loop to avoid modifying a for-loop counter (Sonar S127).
     */
    String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        int i = 0;

        while (i < line.length()) {
            char c = line.charAt(i);
            if (c == '"' && inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                current.append('"');
                i += 2;
            } else if (c == '"') {
                inQuotes = !inQuotes;
                i++;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current.setLength(0);
                i++;
            } else {
                current.append(c);
                i++;
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
