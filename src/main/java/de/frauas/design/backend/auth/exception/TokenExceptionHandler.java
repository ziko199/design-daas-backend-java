package de.frauas.design.backend.auth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Maps any {@link TokenGrantException} thrown by {@code TokenService} to an HTTP response.
 *
 * <p>Scoped to controllers annotated with {@link TokenEndpoint} (currently only
 * {@code TokenController}), so it lives next to the exceptions it handles instead of
 * cluttering the app-wide {@code GlobalExceptionHandler} with token-grant-specific error
 * shapes. Scoping by annotation (rather than {@code assignableTypes}) keeps this exception
 * package free of a direct dependency on the controller layer.</p>
 */
@Slf4j
@RestControllerAdvice(annotations = TokenEndpoint.class)
public class TokenExceptionHandler {

    /**
     * Converts a rejected grant into an OAuth2-style error response, e.g.
     * {@code {"error": "invalid_grant", "error_description": "..."}}.
     *
     * @param ex the exception carrying the HTTP status and OAuth2 error code
     * @return an error response with the status/body defined by {@code ex}
     */
    @ExceptionHandler(TokenGrantException.class)
    public ResponseEntity<Map<String, Object>> handleTokenGrantException(TokenGrantException ex) {
        log.warn("POST /oauth2/user/token — rejected status={} error={}", ex.getStatus(), ex.getError());
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of("error", ex.getError(), "error_description", ex.getMessage()));
    }
}
