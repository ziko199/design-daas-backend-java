package de.frauas.design.backend.auth.exception;

/**
 * The {@code grant_type} request parameter is neither {@code password} nor
 * {@code refresh_token}.
 */
public class UnsupportedGrantTypeException extends TokenGrantException {
    public UnsupportedGrantTypeException() {
        super(400, "unsupported_grant_type", "Only 'password' and 'refresh_token' are supported");
    }
}
