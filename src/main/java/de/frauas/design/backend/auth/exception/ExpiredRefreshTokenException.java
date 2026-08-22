package de.frauas.design.backend.auth.exception;

/**
 * The refresh token was valid but has passed its expiry timestamp.
 */
public class ExpiredRefreshTokenException extends TokenGrantException {
    public ExpiredRefreshTokenException() {
        super(401, "invalid_grant", "Refresh token has expired");
    }
}
