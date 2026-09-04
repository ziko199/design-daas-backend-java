package de.frauas.design.backend.auth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Maps any {@link SessionException} thrown by {@code SessionService} to an HTTP response.
 *
 * <p>Scoped to controllers annotated with {@link SessionEndpoint} (currently only
 * {@code OAuth2SessionController}), so it lives next to the exceptions it handles instead of
 * cluttering the app-wide {@code GlobalExceptionHandler} with session-specific error shapes.
 * Scoping by annotation (rather than {@code assignableTypes}) keeps this exception package free
 * of a direct dependency on the controller layer.</p>
 */
@Slf4j
@RestControllerAdvice(annotations = SessionEndpoint.class)
public class SessionExceptionHandler {

    /**
     * Converts a rejected session check into an OAuth2-style error response, e.g.
     * {@code {"error": "token_revoked", "error_description": "..."}}.
     *
     * @param ex the exception carrying the HTTP status and OAuth2 error code
     * @return an error response with the status/body defined by {@code ex}
     */
    @ExceptionHandler(SessionException.class)
    public ResponseEntity<Map<String, Object>> handleSessionException(SessionException ex) {
        log.warn("GET /oauth2/user/session — rejected status={} error={}", ex.getStatus(), ex.getError());
        return ResponseEntity.status(ex.getStatus())
                .body(Map.of("error", ex.getError(), "error_description", ex.getMessage()));
    }
}
