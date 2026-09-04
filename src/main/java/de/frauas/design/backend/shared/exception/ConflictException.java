package de.frauas.design.backend.shared.exception;

/**
 * Base type for "the requested state change conflicts with the resource's current state"
 * failures — e.g. an association that already exists.
 *
 * <p>Mapped to {@code 409 Conflict} by {@code GlobalExceptionHandler}. Feature packages
 * should extend this class (rather than {@code GlobalExceptionHandler} depending on
 * feature-specific exception types) to keep the {@code shared} package free of
 * dependencies on higher-level packages.</p>
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
