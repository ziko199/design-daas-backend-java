package de.frauas.design.backend.auth.exception;

/**
 * The username is unknown or the supplied password does not match. Both cases are
 * reported identically to avoid leaking whether an account exists.
 */
public class InvalidCredentialsException extends TokenGrantException {
    public InvalidCredentialsException() {
        super(401, "invalid_grant", "Invalid credentials");
    }
}
