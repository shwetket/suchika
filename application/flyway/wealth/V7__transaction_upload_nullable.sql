-- V7__transaction_upload_nullable.sql
-- Allow manual transaction entry (Q7) by relaxing the NOT NULL constraint on
-- wealth.transaction.upload_id. CSV-imported rows continue to carry a real
-- upload_id; manually-entered rows (source = 'MANUAL' in metadata) will have
-- upload_id = NULL. The FK itself stays — NULL simply means no upload record.
ALTER TABLE wealth.transaction
    ALTER COLUMN upload_id DROP NOT NULL;
