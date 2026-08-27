package de.frauas.design.backend.desktop.exception;

import de.frauas.design.backend.shared.exception.ConflictException;

/**
 * The requested {@code UserGroup} is already associated with the target {@code DesktopGroup}.
 *
 * <p>Extends {@link ConflictException} so it is still mapped to {@code 409 Conflict}
 * by the existing {@code GlobalExceptionHandler} without any additional wiring.</p>
 */
public class UserGroupAlreadyAssociatedException extends ConflictException {

    public UserGroupAlreadyAssociatedException(Integer desktopGroupId, Integer userGroupId) {
        super("UserGroup " + userGroupId + " is already associated with DesktopGroup " + desktopGroupId);
    }
}

