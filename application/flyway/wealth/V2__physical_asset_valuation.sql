-- Adds valuation tracking to wealth.physical_asset so non-vehicle assets (real estate,
-- gold jewellery/bonds) can carry a current market value into net worth. Nullable, no
-- default, no CHECK — matches the project's "no CHECK constraints" rule and the existing
-- account.opening_balance/interest_rate pattern of plain nullable typed columns sitting
-- alongside the JSONB metadata column.
ALTER TABLE wealth.physical_asset
    ADD COLUMN current_value  NUMERIC(19,4),
    ADD COLUMN valuation_date DATE;
