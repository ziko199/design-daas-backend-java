package de.frauas.design.backend.auth.exception;

import de.frauas.design.backend.auth.controller.OAuth2SessionController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Maps any {@link SessionException} thrown by {@code SessionService} to an HTTP response.
 *
 * <p>Scoped to {@link OAuth2SessionController} only (via {@code assignableTypes}), so it
 * lives next to the exceptions it handles instead of cluttering the app-wide
 * {@code GlobalExceptionHandler} with session-specific error shapes.</p>
 */
@Slf4j
@RestControllerAdvice(assignableTypes = OAuth2SessionController.class)
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
