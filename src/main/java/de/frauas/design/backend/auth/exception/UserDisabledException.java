package de.frauas.design.backend.auth.exception;

/**
 * The user behind the token exists but their account has been disabled.
 */
public class UserDisabledException extends SessionException {
    public UserDisabledException() {
        super(401, "user_disabled", "User account is disabled");
    }
}
