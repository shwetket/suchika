package com.suchika.wealth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ExpenseCategoryTest {

    @Test
    void values_matchDocumentedFiveValueContract() {
        ExpenseCategory[] values = ExpenseCategory.values();

        assertEquals(5, values.length);
        assertEquals(ExpenseCategory.HOUSEHOLD_CORE, ExpenseCategory.valueOf("HOUSEHOLD_CORE"));
        assertEquals(ExpenseCategory.CHILD_RELATED, ExpenseCategory.valueOf("CHILD_RELATED"));
        assertEquals(ExpenseCategory.MAINTENANCE, ExpenseCategory.valueOf("MAINTENANCE"));
        assertEquals(ExpenseCategory.DISCRETIONARY, ExpenseCategory.valueOf("DISCRETIONARY"));
        assertEquals(ExpenseCategory.UNCATEGORIZED, ExpenseCategory.valueOf("UNCATEGORIZED"));
    }
}
