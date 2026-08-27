-- V4__user_lockout.sql
-- Adds brute-force / account-lockout fields to the users table (SEC-H3).
-- failed_login_attempts: incremented on every bad-password attempt; reset on success.
-- locked_until: non-null while the account is temporarily locked.

ALTER TABLE users
    ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
    ADD COLUMN locked_until          TIMESTAMP NULL     DEFAULT NULL;
