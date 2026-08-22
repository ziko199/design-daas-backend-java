package de.frauas.design.backend.auth.exception;

import lombok.Getter;

/**
 * Base type for every reason {@code GET /oauth2/user/session} may reject a JWT
 * (revoked token, unknown/disabled user, ...).
 *
 * <p>Each failure reason gets its own concrete subclass ({@link TokenRevokedException},
 * {@link UserNotFoundException}, {@link UserDisabledException}) instead of one generic
 * class instantiated with different status/error arguments. That makes each failure a
 * distinct, named type that's easy to throw, catch and unit-test individually, while
 * {@link SessionExceptionHandler} only needs to know about this common base class to map
 * any of them to the right HTTP response.</p>
 */
@Getter
public abstract class SessionException extends RuntimeException {

    /**
     * HTTP status to respond with, e.g. 401.
     */
    private final int status;

    /**
     * Short machine-readable OAuth2-style error code, e.g. {@code "token_revoked"}.
     */
    private final String error;

    protected SessionException(int status, String error, String description) {
        super(description);
        this.status = status;
        this.error = error;
    }
}
