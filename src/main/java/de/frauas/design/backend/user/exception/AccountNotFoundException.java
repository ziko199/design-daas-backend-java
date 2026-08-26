package de.frauas.design.backend.user.exception;

import java.util.NoSuchElementException;

/**
 * No account exists with the requested ID (used where the caller does not care whether
 * the account is a {@code User} or an {@code Admin}, e.g. resolving the JWT subject).
 *
 * <p>Extends {@link NoSuchElementException} so it is still mapped to {@code 404 Not Found}
 * by the existing {@code GlobalExceptionHandler} without any additional wiring.</p>
 */
public class AccountNotFoundException extends NoSuchElementException {

    public AccountNotFoundException(Integer id) {
        super("User not found: " + id);
    }
}
