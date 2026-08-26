package de.frauas.design.backend.user.exception;

import java.util.NoSuchElementException;

/**
 * No {@code User} exists with the requested ID.
 *
 * <p>Extends {@link NoSuchElementException} so it is still mapped to {@code 404 Not Found}
 * by the existing {@code GlobalExceptionHandler} without any additional wiring.</p>
 */
public class UserNotFoundException extends NoSuchElementException {

    public UserNotFoundException(Integer id) {
        super("User not found: " + id);
    }
}
