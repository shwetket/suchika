-- The dedup UNIQUE constraint included description (5 fields), but the real
-- business rule (existsByDeduplicationKey, used by CSV upload since the v0.4
-- dedup fix) is 4 fields with description deliberately excluded. The 5-field
-- constraint is narrower than intended and can let real duplicates through.

ALTER TABLE wealth.transaction DROP CONSTRAINT uq_transaction_dedup;

ALTER TABLE wealth.transaction
    ADD CONSTRAINT uq_transaction_dedup
        UNIQUE (account_id, txn_date, amount, txn_type);
