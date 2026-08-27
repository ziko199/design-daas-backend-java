-- ============================================================
-- V1__init.sql  –  Design DaaS Backend schema
-- ============================================================

-- ---- Application tables ----

CREATE TABLE users
(
    id                        INT AUTO_INCREMENT PRIMARY KEY,
    role                      VARCHAR(16)  NOT NULL,
    guid                      VARCHAR(255) NOT NULL,
    name                      VARCHAR(255) NOT NULL,
    email                     VARCHAR(255) NOT NULL UNIQUE,
    password                  VARCHAR(255) NOT NULL,
    enabled                   BOOLEAN      NOT NULL DEFAULT FALSE,
    registration_code         VARCHAR(8),
    registration_code_timeout DATETIME,
    registration_used_moment  DATETIME
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

-- ---- Spring Authorization Server tables ----

CREATE TABLE oauth2_registered_client
(
    id                            VARCHAR(100)                            NOT NULL,
    client_id                     VARCHAR(100)                            NOT NULL,
    client_id_issued_at           TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    client_secret                 VARCHAR(200)  DEFAULT NULL,
    client_secret_expires_at      TIMESTAMP     DEFAULT NULL,
    client_name                   VARCHAR(200)                            NOT NULL,
    client_authentication_methods VARCHAR(1000)                           NOT NULL,
    authorization_grant_types     VARCHAR(1000)                           NOT NULL,
    redirect_uris                 VARCHAR(1000) DEFAULT NULL,
    post_logout_redirect_uris     VARCHAR(1000) DEFAULT NULL,
    scopes                        VARCHAR(1000)                           NOT NULL,
    client_settings               VARCHAR(2000)                           NOT NULL,
    token_settings                VARCHAR(2000)                           NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE oauth2_authorization_consent
(
    registered_client_id VARCHAR(100)  NOT NULL,
    principal_name       VARCHAR(200)  NOT NULL,
    authorities          VARCHAR(1000) NOT NULL,
    PRIMARY KEY (registered_client_id, principal_name)
);

CREATE TABLE oauth2_authorization
(
    id                            VARCHAR(100) NOT NULL,
    registered_client_id          VARCHAR(100) NOT NULL,
    principal_name                VARCHAR(200) NOT NULL,
    authorization_grant_type      VARCHAR(100) NOT NULL,
    authorized_scopes             VARCHAR(1000) DEFAULT NULL,
    attributes                    BLOB          DEFAULT NULL,
    state                         VARCHAR(500)  DEFAULT NULL,
    authorization_code_value      BLOB          DEFAULT NULL,
    authorization_code_issued_at  TIMESTAMP     DEFAULT NULL,
    authorization_code_expires_at TIMESTAMP     DEFAULT NULL,
    authorization_code_metadata   BLOB          DEFAULT NULL,
    access_token_value            BLOB          DEFAULT NULL,
    access_token_issued_at        TIMESTAMP     DEFAULT NULL,
    access_token_expires_at       TIMESTAMP     DEFAULT NULL,
    access_token_metadata         BLOB          DEFAULT NULL,
    access_token_type             VARCHAR(100)  DEFAULT NULL,
    access_token_scopes           VARCHAR(1000) DEFAULT NULL,
    oidc_id_token_value           BLOB          DEFAULT NULL,
    oidc_id_token_issued_at       TIMESTAMP     DEFAULT NULL,
    oidc_id_token_expires_at      TIMESTAMP     DEFAULT NULL,
    oidc_id_token_metadata        BLOB          DEFAULT NULL,
    refresh_token_value           BLOB          DEFAULT NULL,
    refresh_token_issued_at       TIMESTAMP     DEFAULT NULL,
    refresh_token_expires_at      TIMESTAMP     DEFAULT NULL,
    refresh_token_metadata        BLOB          DEFAULT NULL,
    user_code_value               BLOB          DEFAULT NULL,
    user_code_issued_at           TIMESTAMP     DEFAULT NULL,
    user_code_expires_at          TIMESTAMP     DEFAULT NULL,
    user_code_metadata            BLOB          DEFAULT NULL,
    device_code_value             BLOB          DEFAULT NULL,
    device_code_issued_at         TIMESTAMP     DEFAULT NULL,
    device_code_expires_at        TIMESTAMP     DEFAULT NULL,
    device_code_metadata          BLOB          DEFAULT NULL,
    PRIMARY KEY (id)
);
