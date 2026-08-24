package de.frauas.design.backend.auth.exception;

/**
 * No JWT was presented on the request (defensive only — under the normal security
 * filter chain, an unauthenticated request to a protected endpoint is already
 * rejected before reaching the controller).
 */
public class MissingTokenException extends SessionException {
    public MissingTokenException() {
        super(401, "missing_token", "No access token present");
    }
}
