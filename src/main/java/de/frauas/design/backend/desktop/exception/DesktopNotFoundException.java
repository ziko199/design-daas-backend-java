package de.frauas.design.backend.desktop.exception;

import java.util.NoSuchElementException;

/**
 * No {@code Desktop} exists with the requested ID.
 *
 * <p>Extends {@link NoSuchElementException} so it is still mapped to {@code 404 Not Found}
 * by the existing {@code GlobalExceptionHandler} without any additional wiring.</p>
 */
public class DesktopNotFoundException extends NoSuchElementException {

    public DesktopNotFoundException(Integer id) {
        super("Desktop not found: " + id);
    }
}
