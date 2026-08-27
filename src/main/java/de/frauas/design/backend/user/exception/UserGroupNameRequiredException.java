package de.frauas.design.backend.user.exception;

/**
 * A user group was created without a name and without a description to fall back on.
 *
 * <p>Extends {@link IllegalArgumentException} so it is still mapped to {@code 400 Bad Request}
 * by the existing {@code GlobalExceptionHandler} without any additional wiring.</p>
 */
public class UserGroupNameRequiredException extends IllegalArgumentException {

    public UserGroupNameRequiredException() {
        super("UserGroup requires a name or a description");
    }
}
