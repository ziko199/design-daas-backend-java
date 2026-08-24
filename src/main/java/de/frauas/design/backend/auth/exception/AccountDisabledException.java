package de.frauas.design.backend.auth.exception;

/**
 * The user's credentials are valid but their account has been disabled.
 */
public class AccountDisabledException extends TokenGrantException {
    public AccountDisabledException() {
        super(401, "invalid_grant", "User account is disabled");
    }
}
