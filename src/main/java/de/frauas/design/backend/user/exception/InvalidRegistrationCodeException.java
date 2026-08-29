package de.frauas.design.backend.user.exception;

/**
 * The supplied email-verification registration code is missing, incorrect, expired,
 * or does not correspond to a known regular-user account.
 *
 * <p>Extends {@link IllegalArgumentException} so it is mapped to {@code 400 Bad Request}
 * by the existing {@code GlobalExceptionHandler} without any additional wiring.</p>
 */
public class InvalidRegistrationCodeException extends IllegalArgumentException {

    public InvalidRegistrationCodeException() {
        super("Registration code is invalid, expired, or does not match the given email");
    }
}
