package de.frauas.design.backend.auth.exception;

import lombok.Getter;

/**
 * Base type for every reason {@code POST /oauth2/user/token} may reject a password
 * or refresh-token grant (bad credentials, locked/disabled account, scope escalation,
 * invalid/expired refresh token, ...).
 *
 * <p>Each failure reason gets its own concrete subclass instead of one generic class
 * instantiated with different status/error arguments. That makes each failure a
 * distinct, named type that's easy to throw, catch and unit-test individually, while
 * {@link TokenExceptionHandler} only needs to know about this common base class to map
 * any of them to the right HTTP response.</p>
 */
@Getter
public abstract class TokenGrantException extends RuntimeException {

    /**
     * HTTP status to respond with, e.g. 401.
     */
    private final int status;

    /**
     * Short machine-readable OAuth2-style error code, e.g. {@code "invalid_grant"}.
     */
    private final String error;

    protected TokenGrantException(int status, String error, String description) {
        super(description);
        this.status = status;
        this.error = error;
    }
}
