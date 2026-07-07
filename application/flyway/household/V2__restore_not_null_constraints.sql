-- Restores NOT NULL on three columns that silently lost it during the
-- V1 consolidation rewrite (2026-07-04/05). Per the project's DB constraint
-- philosophy (CLAUDE.md): NOT NULL/PK/FK/UNIQUE are structural invariants
-- that belong in the DB; only CHECK constraints were meant to be dropped.
-- All three are already always set by domain-layer create() validation, so
-- no backfill is needed pre-v1.0 (ephemeral data per project convention).

ALTER TABLE household.inventory_item
    ALTER COLUMN unit SET NOT NULL,
    ALTER COLUMN source_platform SET NOT NULL;

ALTER TABLE household.goal
    ALTER COLUMN status SET DEFAULT 'ACTIVE',
    ALTER COLUMN status SET NOT NULL;
