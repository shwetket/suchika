package com.suchika.wealth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins the exact 11-value account type contract (Java enum + {@code wealth.yaml}
 * must both list the same values — see documents/domain-state/wealth.md).
 * A future accidental removal/reorder of a value would fail this test.
 */
class AccountTypeTest {

    @Test
    void values_matchDocumentedElevenValueContract() {
        AccountType[] values = AccountType.values();

        assertEquals(11, values.length);
        assertEquals(AccountType.SAVINGS, AccountType.valueOf("SAVINGS"));
        assertEquals(AccountType.CURRENT, AccountType.valueOf("CURRENT"));
        assertEquals(AccountType.CREDIT_CARD, AccountType.valueOf("CREDIT_CARD"));
        assertEquals(AccountType.HOME_LOAN, AccountType.valueOf("HOME_LOAN"));
        assertEquals(AccountType.PERSONAL_LOAN, AccountType.valueOf("PERSONAL_LOAN"));
        assertEquals(AccountType.CAR_LOAN, AccountType.valueOf("CAR_LOAN"));
        assertEquals(AccountType.MUTUAL_FUND, AccountType.valueOf("MUTUAL_FUND"));
        assertEquals(AccountType.NPS, AccountType.valueOf("NPS"));
        assertEquals(AccountType.PPF, AccountType.valueOf("PPF"));
        assertEquals(AccountType.FD, AccountType.valueOf("FD"));
        assertEquals(AccountType.EPF, AccountType.valueOf("EPF"));
    }
}
