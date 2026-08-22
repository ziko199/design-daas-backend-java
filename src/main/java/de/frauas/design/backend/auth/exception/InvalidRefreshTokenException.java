package de.frauas.design.backend.auth.exception;

/**
 * The presented refresh token is unknown, malformed, or has already been used
 * (rotated away) once before.
 */
public class InvalidRefreshTokenException extends TokenGrantException {
    public InvalidRefreshTokenException() {
        super(401, "invalid_grant", "Invalid or already used refresh token");
    }
}
