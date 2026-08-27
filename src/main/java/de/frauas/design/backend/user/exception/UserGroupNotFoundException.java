package de.frauas.design.backend.user.exception;

import java.util.NoSuchElementException;

/**
 * No {@code UserGroup} exists with the requested ID.
 *
 * <p>Extends {@link NoSuchElementException} so it is still mapped to {@code 404 Not Found}
 * by the existing {@code GlobalExceptionHandler} without any additional wiring.</p>
 */
public class UserGroupNotFoundException extends NoSuchElementException {

    public UserGroupNotFoundException(Integer id) {
        super("UserGroup not found: " + id);
    }
}
