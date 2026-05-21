package com.suchika.wealth.adapters.services;

import com.suchika.shared.exception.ApplicationException;

import java.util.Arrays;
import java.util.List;

public class CsvParseException extends ApplicationException {

    private static final int STATUS = 400;
    private static final String CODE = "BAD_REQUEST";

    private final String errorType;
    private final List<String> missingColumns;

    private CsvParseException(String errorType, List<String> missingColumns, String message) {
        super(STATUS, CODE, message);
        this.errorType = errorType;
        this.missingColumns = missingColumns;
    }

    public String getErrorType() {
        return errorType;
    }

    public List<String> getMissingColumns() {
        return missingColumns;
    }

    public static CsvParseException missingDateColumn(String[] foundHeaders) {
        return new CsvParseException(
                "MISSING_DATE_COLUMN",
                List.of("date"),
                "Could not identify a date column. Searched for: [Date, Txn Date, Transaction Date, Value Date, Posting Date, Value Dt]. "
                        + "Headers found in file: " + Arrays.toString(foundHeaders) + ".");
    }

    public static CsvParseException missingAmountColumn(String[] foundHeaders) {
        return new CsvParseException(
                "MISSING_AMOUNT_COLUMN",
                List.of("amount", "withdrawal amt.", "deposit amt."),
                "Could not identify an amount column. Searched for debit, credit, or amount headers. "
                        + "Headers found in file: " + Arrays.toString(foundHeaders) + ".");
    }

    public static CsvParseException missingRequiredColumn(String columnName, String[] foundHeaders) {
        return new CsvParseException(
                "MISSING_REQUIRED_COLUMN",
                List.of(columnName),
                "Required column '" + columnName + "' not found. "
                        + "Headers found in file: " + Arrays.toString(foundHeaders) + ".");
    }
}
