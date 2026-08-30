-- ============================================================
-- V1__init.sql  –  Design DaaS Backend schema
-- ============================================================

-- ---- Application tables ----

CREATE TABLE users
(
    id                        INT AUTO_INCREMENT PRIMARY KEY,
    role                      VARCHAR(16)  NOT NULL,
    name                      VARCHAR(255) NOT NULL,
    email                     VARCHAR(255) NOT NULL UNIQUE,
    password                  VARCHAR(255) NOT NULL,
    enabled                   BOOLEAN      NOT NULL DEFAULT FALSE,
    registration_code         VARCHAR(8),
    registration_code_timeout DATETIME,
    registration_used_moment  DATETIME,
    failed_login_attempts     INT          NOT NULL DEFAULT 0,
    locked_until              TIMESTAMP    NULL     DEFAULT NULL
);

CREATE TABLE user_groups
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE users_to_user_groups
(
    user_id       INT NOT NULL,
    user_group_id INT NOT NULL,
    PRIMARY KEY (user_id, user_group_id),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (user_group_id) REFERENCES user_groups (id) ON DELETE CASCADE
);

CREATE TABLE desktop_groups
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE desktops
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE desktops_to_desktop_groups
(
    desktop_id       INT NOT NULL,
    desktop_group_id INT NOT NULL,
    PRIMARY KEY (desktop_id, desktop_group_id),
    FOREIGN KEY (desktop_id) REFERENCES desktops (id) ON DELETE CASCADE,
    FOREIGN KEY (desktop_group_id) REFERENCES desktop_groups (id) ON DELETE CASCADE
);

CREATE TABLE user_group_to_desktop_group
(
    user_group_id    INT NOT NULL,
    desktop_group_id INT NOT NULL,
    PRIMARY KEY (user_group_id, desktop_group_id),
    FOREIGN KEY (user_group_id) REFERENCES user_groups (id) ON DELETE CASCADE,
    FOREIGN KEY (desktop_group_id) REFERENCES desktop_groups (id) ON DELETE CASCADE
);

CREATE TABLE endpoints
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    function_name VARCHAR(255) NOT NULL UNIQUE,
    description   VARCHAR(255)
);

CREATE TABLE endpoint_groups
(
    id          INT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE endpoint_group_members
(
    endpoint_group_id INT NOT NULL,
    endpoint_id       INT NOT NULL,
    PRIMARY KEY (endpoint_group_id, endpoint_id),
    FOREIGN KEY (endpoint_group_id) REFERENCES endpoint_groups (id) ON DELETE CASCADE,
    FOREIGN KEY (endpoint_id) REFERENCES endpoints (id) ON DELETE CASCADE
);

CREATE TABLE endpoint_user_access
(
    id           INT AUTO_INCREMENT PRIMARY KEY,
    user_id      INT     NOT NULL,
    endpoint_id  INT     NOT NULL,
    allow_access BOOLEAN NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (endpoint_id) REFERENCES endpoints (id) ON DELETE CASCADE
);

CREATE TABLE endpoint_user_group_access
(
    id            INT AUTO_INCREMENT PRIMARY KEY,
    user_group_id INT     NOT NULL,
    endpoint_id   INT     NOT NULL,
    allow_access  BOOLEAN NOT NULL,
    FOREIGN KEY (user_group_id) REFERENCES user_groups (id) ON DELETE CASCADE,
    FOREIGN KEY (endpoint_id) REFERENCES endpoints (id) ON DELETE CASCADE
);

CREATE TABLE endpoint_group_user_access
(
    id                INT AUTO_INCREMENT PRIMARY KEY,
    user_id           INT     NOT NULL,
    endpoint_group_id INT     NOT NULL,
    allow_access      BOOLEAN NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    FOREIGN KEY (endpoint_group_id) REFERENCES endpoint_groups (id) ON DELETE CASCADE
);

CREATE TABLE endpoint_group_user_group_access
(
    id                INT AUTO_INCREMENT PRIMARY KEY,
    user_group_id     INT     NOT NULL,
    endpoint_group_id INT     NOT NULL,
    allow_access      BOOLEAN NOT NULL,
    FOREIGN KEY (user_group_id) REFERENCES user_groups (id) ON DELETE CASCADE,
    FOREIGN KEY (endpoint_group_id) REFERENCES endpoint_groups (id) ON DELETE CASCADE
);

-- ---- Auth tables (custom JWT issuance, not Spring Authorization Server) ----

CREATE TABLE refresh_tokens
(
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    token_value VARCHAR(255) NOT NULL UNIQUE,
    user_id     INT          NOT NULL,
    scope       VARCHAR(20)  NOT NULL,
    created_at  TIMESTAMP    NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at  TIMESTAMP(6) NULL,
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE TABLE access_tokens
(
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    jti        VARCHAR(200) NOT NULL UNIQUE,
    user_id    INT          NOT NULL,
    scope      VARCHAR(20)  NOT NULL,
    issued_at  TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    revoked    BOOLEAN      NOT NULL DEFAULT FALSE,
    revoked_at TIMESTAMP(6) NULL,
    INDEX      idx_access_tokens_user_id (user_id),
    INDEX      idx_access_tokens_expires_at (expires_at),
    FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);
