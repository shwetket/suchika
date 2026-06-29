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
 * <p>Supports two column layouts:
 * <ol>
 *   <li>Split debit/credit: separate Withdrawal/Deposit or Debit/Credit columns (HDFC savings).
 *   <li>Single amount: one Amount column; negative values → DEBIT, positive → CREDIT.
 * </ol>
 *
 * <p>All detected headers are matched case-insensitively after trimming.
 * Amount values are normalised to absolute (positive) before storage; direction is in TxnType.
 */
@ApplicationScoped
public class StatementCsvParser {

    public record ParsedRow(LocalDate date, BigDecimal amount, TxnType txnType, String description) {
        public ParsedRow withDescription(String newDescription) {
            return new ParsedRow(date, amount, txnType, newDescription);
        }
    }

    private static final List<String> DATE_HEADERS = List.of(
            "date", "txn date", "transaction date", "value date", "posting date", "value dt"
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

    private static final List<DateTimeFormatter> DATE_FORMATS = List.of(
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd MMM yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yy")
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

        String[] headers = parseCsvLine(lines.get(0));
        ColumnMap cols = detectColumns(headers);

        List<ParsedRow> rows = new ArrayList<>();
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            String[] fields = parseCsvLine(line);
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

    private record ColumnMap(int dateCol, int descriptionCol, Integer debitCol, Integer creditCol, Integer amountCol) {
        boolean isSplitLayout() {
            return debitCol != null || creditCol != null;
        }
    }

    private ColumnMap detectColumns(String[] headers) {
        Integer dateCol = findColumn(headers, DATE_HEADERS);
        Integer descriptionCol = findColumn(headers, DESCRIPTION_HEADERS);
        Integer debitCol = findColumn(headers, DEBIT_HEADERS);
        Integer creditCol = findColumn(headers, CREDIT_HEADERS);
        Integer amountCol = findColumn(headers, AMOUNT_HEADERS);

        if (dateCol == null) {
            throw CsvParseException.missingDateColumn(headers);
        }
        if (descriptionCol == null) {
            throw CsvParseException.missingRequiredColumn("description", headers);
        }
        if (debitCol == null && creditCol == null && amountCol == null) {
            throw CsvParseException.missingAmountColumn(headers);
        }

        return new ColumnMap(dateCol, descriptionCol, debitCol, creditCol, amountCol);
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

        if (cols.isSplitLayout()) {
            return parseSplitAmountRow(fields, cols, date, description);
        } else {
            return parseSingleAmountRow(fields, cols, date, description);
        }
    }

    private ParsedRow parseSplitAmountRow(String[] fields, ColumnMap cols, LocalDate date, String description) {
        String rawDebit = cols.debitCol() != null ? safeGet(fields, cols.debitCol()) : null;
        String rawCredit = cols.creditCol() != null ? safeGet(fields, cols.creditCol()) : null;

        BigDecimal debit = parseAmount(rawDebit);
        BigDecimal credit = parseAmount(rawCredit);

        if (debit != null && debit.compareTo(BigDecimal.ZERO) > 0) {
            return new ParsedRow(date, debit.abs(), TxnType.DEBIT, description);
        }
        if (credit != null && credit.compareTo(BigDecimal.ZERO) > 0) {
            return new ParsedRow(date, credit.abs(), TxnType.CREDIT, description);
        }
        return null;
    }

    private ParsedRow parseSingleAmountRow(String[] fields, ColumnMap cols, LocalDate date, String description) {
        if (cols.amountCol() == null) return null;
        String rawAmount = safeGet(fields, cols.amountCol());
        BigDecimal amount = parseAmount(rawAmount);
        if (amount == null) return null;

        TxnType txnType = amount.compareTo(BigDecimal.ZERO) < 0 ? TxnType.DEBIT : TxnType.CREDIT;
        return new ParsedRow(date, amount.abs(), txnType, description);
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
