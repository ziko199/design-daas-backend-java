package de.frauas.design.backend.auth.exception;

/** The JWT's {@code sub} claim does not match any known user (account deleted after issuance). */
public class UserNotFoundException extends SessionException {
    public UserNotFoundException() {
        super(401, "invalid_token", "User not found for token subject");
    }
}
