package de.frauas.design.backend.auth.exception;

/**
 * The presented JWT's access token is unknown or has been revoked (e.g. via logout).
 */
public class TokenRevokedException extends SessionException {
    public TokenRevokedException() {
        super(401, "token_revoked", "The access token has been revoked");
    }
}
