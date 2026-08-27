-- V6__single_scope_column.sql
-- Each user has exactly one role, so a token only ever carries a single scope.
-- Renames the "scopes" columns to "scope" and tightens their length accordingly.

ALTER TABLE access_tokens
    CHANGE COLUMN scopes scope VARCHAR (20) NOT NULL;

-- refresh_tokens.scopes was nullable; backfill before enforcing NOT NULL
UPDATE refresh_tokens
SET scopes = 'user'
WHERE scopes IS NULL;

ALTER TABLE refresh_tokens
    CHANGE COLUMN scopes scope VARCHAR (20) NOT NULL;

-- replaced_by was written on rotation but never read anywhere (no theft-detection
-- logic actually consumes the chain) — dead column, drop it.
ALTER TABLE refresh_tokens
    DROP COLUMN replaced_by;
