-- ==============================================================================
-- V3__remove_enum_constraints.sql
-- Removes all VARCHAR discriminator CHECK constraints from the household schema.
--
-- REASON: See CLAUDE.md "DB Constraint Philosophy".
-- enum values (event types, platforms, units, goal status) are enforced at the
-- OpenAPI contract + Java layer. No DB migration needed to add a new value.
--
-- WHAT IS REMOVED (enum discriminators):
--   chk_event_type      — PERSONAL/FAMILY/etc. on household.calendar_event
--   chk_source_platform — INSTAMART/BLINKIT/etc. on household.inventory_item
--   chk_unit            — KG/G/L/etc. on household.inventory_item
--   chk_goal_status     — ACTIVE/ACHIEVED/PAUSED on household.goal
--
-- WHAT STAYS (structural invariants):
--   chk_event_dates          — end_date >= start_date (ordering rule, not an enum)
--   chk_quantity_positive    — quantity > 0 (data integrity, not an enum)
--   chk_goal_target_positive — target_amount > 0 (data integrity)
--   chk_goal_current_non_negative — current_amount >= 0 (data integrity)
--   All PK/FK constraints
--
-- INDEXES REMOVED (all household tables are small — seq scan is faster):
--   idx_event_start_date   — household.calendar_event: max dozens of events/year
--   idx_item_category      — household.inventory_item: small table, category filter is fast without index
--   idx_goal_profile_status — household.goal: max handful of goals
--
-- INDEXES KEPT (these serve useful query patterns even at small scale):
--   idx_event_profile_start — "events for person X by date" — the primary UI query
--   idx_item_profile_date   — "items consumed by member X" — primary query
--   idx_item_metadata       — JSONB traceability queries (order IDs, platform refs)
-- ==============================================================================


-- ---- household.calendar_event ----
ALTER TABLE household.calendar_event
    DROP CONSTRAINT IF EXISTS chk_event_type;


-- ---- household.inventory_item ----
ALTER TABLE household.inventory_item
    DROP CONSTRAINT IF EXISTS chk_source_platform,
    DROP CONSTRAINT IF EXISTS chk_unit;


-- ---- household.goal ----
ALTER TABLE household.goal
    DROP CONSTRAINT IF EXISTS chk_goal_status;


-- ---- Remove indexes on small tables ----
DROP INDEX IF EXISTS household.idx_event_start_date;
DROP INDEX IF EXISTS household.idx_item_category;
DROP INDEX IF EXISTS household.idx_goal_profile_status;
