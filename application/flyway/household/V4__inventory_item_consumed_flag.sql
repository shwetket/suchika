-- ==============================================================================
-- V4__inventory_item_consumed_flag.sql
-- Adds is_consumed to household.inventory_item.
--
-- SEMANTICS (product owner resolution, v0.5 Phase 0, Q6):
--   is_consumed = TRUE means "this record has been successfully used in a
--   calculation" (e.g. rolled into a supply/spend analytics computation) —
--   it does NOT mean "the grocery item was physically used up."
--
--   Inventory rows are never deleted and never expire. All history stays
--   available permanently to support year-over-year tracking from the start
--   of usage. This flag is purely a computation-consumed marker, toggled by
--   the same PUT /v1/inventory-items/{id} endpoint used for general edits.
--
-- Plain boolean, not a discriminator column — no CHECK constraint needed
-- (see CLAUDE.md "DB Constraint Philosophy": booleans are structural, not
-- enum discriminators, but a two-value boolean needs no CHECK regardless).
-- ==============================================================================

ALTER TABLE household.inventory_item
    ADD COLUMN is_consumed BOOLEAN NOT NULL DEFAULT false;

COMMENT ON COLUMN household.inventory_item.is_consumed IS $$
Marks whether this inventory record has been successfully used in a
calculation (e.g. supply/spend analytics). Does NOT mean the physical item
was consumed/used up. Inventory rows are never deleted and never expire —
this flag supports year-over-year tracking without altering the permanent
ledger semantics.
$$;
