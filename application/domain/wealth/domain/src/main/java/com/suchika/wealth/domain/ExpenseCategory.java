package com.suchika.wealth.domain;

/**
 * DEBIT-shaped categories (original 5) plus income-shaped categories added for
 * ADR-022's THIRTY_SEVENTY_TARGET formula, which needs to identify income-tagged
 * CREDIT transactions. Never validated against txn_type at the domain layer —
 * any category can be set on any transaction (unchanged from the original 5).
 */
public enum ExpenseCategory {
    HOUSEHOLD_CORE, CHILD_RELATED, MAINTENANCE, DISCRETIONARY, UNCATEGORIZED,
    SALARY, RENTAL, OTHER_INCOME
}
