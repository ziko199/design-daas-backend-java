package de.frauas.design.backend.auth.exception;

/**
 * The client requested a scope that its user role is not authorized to
 * receive (scope-escalation attempt).
 */
public class InvalidScopeException extends TokenGrantException {
    public InvalidScopeException() {
        super(400, "invalid_scope", "One or more requested scopes are not authorized for this user");
    }
}
