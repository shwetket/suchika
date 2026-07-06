-- profile.profile.metadata was never wired up: not mapped in ProfileEntity,
-- not in application/contract/profile.yaml, not in any DTO or service code.
-- Dropping it now, pre-v1.0, rather than carrying a dead column into
-- persistent schema. Re-add via a new migration if a real use (e.g. avatar
-- storage) comes along.

ALTER TABLE profile.profile DROP COLUMN metadata;
