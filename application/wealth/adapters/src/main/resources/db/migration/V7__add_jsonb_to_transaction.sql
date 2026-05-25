-- PHASE 3: JSONB Transaction Schema Optimization
-- Add metadata JSONB column and restructure transaction table

-- Add JSONB metadata column for flexible transaction attributes
ALTER TABLE transaction ADD COLUMN metadata JSONB DEFAULT '{}'::jsonb;

-- Remove sparse asset columns (for investments)
ALTER TABLE transaction DROP COLUMN IF EXISTS units;
ALTER TABLE transaction DROP COLUMN IF EXISTS nav;

-- Remove sparse loan EMI columns  
ALTER TABLE transaction DROP COLUMN IF EXISTS principal_component;
ALTER TABLE transaction DROP COLUMN IF EXISTS interest_component;

-- Create index on JSONB metadata for efficient queries
CREATE INDEX idx_txn_metadata ON transaction USING gin(metadata);

-- Ensure core columns remain for all transaction types
-- Core columns: id, account_id, txn_type, amount, txn_date, description, transfer_to_account_id, created_at
-- Metadata JSONB stores: units, nav, principal_component, interest_component, and any other transaction-specific attributes
