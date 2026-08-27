-- V3__access_tokens.sql
-- Adds access_tokens table (mirrors PHP oauth2_access_token) for JWT revocation.
-- Extends refresh_tokens with rotation / revocation support.

CREATE TABLE access_tokens
(
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    jti        VARCHAR(200) NOT NULL UNIQUE,
    user_id    INT          NOT NULL,
    scopes     VARCHAR(500) NOT NULL,
    issued_at  TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP(6) NULL,
    INDEX      idx_access_tokens_user_id (user_id),
    INDEX      idx_access_tokens_expires_at (expires_at),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

ALTER TABLE refresh_tokens
    ADD COLUMN revoked BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN revoked_at  TIMESTAMP(6) NULL,
    ADD COLUMN replaced_by VARCHAR(255) NULL;

