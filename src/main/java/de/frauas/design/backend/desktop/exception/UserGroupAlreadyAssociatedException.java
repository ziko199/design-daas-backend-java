package de.frauas.design.backend.desktop.exception;

/**
 * The requested {@code UserGroup} is already associated with the target {@code DesktopGroup}.
 *
 * <p>Mapped to {@code 409 Conflict} by {@code GlobalExceptionHandler}.</p>
 */
public class UserGroupAlreadyAssociatedException extends RuntimeException {

    public UserGroupAlreadyAssociatedException(Integer desktopGroupId, Integer userGroupId) {
        super("UserGroup " + userGroupId + " is already associated with DesktopGroup " + desktopGroupId);
    }
}
