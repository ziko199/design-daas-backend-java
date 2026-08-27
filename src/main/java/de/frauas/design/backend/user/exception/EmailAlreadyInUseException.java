package de.frauas.design.backend.user.exception;

/**
 * The requested email address is already registered to a different account.
 *
 * <p>Extends {@link IllegalArgumentException} so it is still mapped to {@code 400 Bad Request}
 * by the existing {@code GlobalExceptionHandler} without any additional wiring.</p>
 */
public class EmailAlreadyInUseException extends IllegalArgumentException {

    public EmailAlreadyInUseException() {
        super("Email already in use");
    }
}
