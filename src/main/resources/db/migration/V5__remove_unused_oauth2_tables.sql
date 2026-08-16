-- V5__remove_unused_oauth2_tables.sql
-- These three tables were included in V1 for Spring Authorization Server, which
-- is not used in this project. The token endpoint is a plain @RestController with
-- custom JWT issuance. The tables are never populated or read.
DROP TABLE IF EXISTS oauth2_authorization;
DROP TABLE IF EXISTS oauth2_authorization_consent;
DROP TABLE IF EXISTS oauth2_registered_client;
