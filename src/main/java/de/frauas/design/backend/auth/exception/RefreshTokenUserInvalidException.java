package de.frauas.design.backend.auth.exception;

/**
 * The user behind a refresh token no longer exists or has been disabled since the
 * token was issued (mirrors the PHP {@code VerifyingRefreshTokenGrant} behaviour).
 */
public class RefreshTokenUserInvalidException extends TokenGrantException {
    public RefreshTokenUserInvalidException() {
        super(401, "invalid_grant", "User not found or disabled");
    }
}
