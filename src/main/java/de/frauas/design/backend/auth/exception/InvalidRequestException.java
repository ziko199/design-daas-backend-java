package de.frauas.design.backend.auth.exception;

/**
 * A required grant parameter (e.g. {@code username}/{@code password} for the password
 * grant, or {@code refresh_token} for the refresh-token grant) was not supplied.
 */
public class InvalidRequestException extends TokenGrantException {
    public InvalidRequestException(String description) {
        super(400, "invalid_request", description);
    }
}
