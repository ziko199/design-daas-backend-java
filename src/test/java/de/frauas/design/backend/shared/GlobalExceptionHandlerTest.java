package de.frauas.design.backend.shared;

import de.frauas.design.backend.shared.dto.ErrorResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleNotFound returns 404 with message")
    void handleNotFound_returns404() {
        ResponseEntity<ErrorResponse> resp = handler.handleNotFound(new NoSuchElementException("User not found: 99"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getMessage()).isEqualTo("User not found: 99");
    }

    @Test
    @DisplayName("handleBadRequest returns 400 with message")
    void handleBadRequest_returns400() {
        ResponseEntity<ErrorResponse> resp =
                handler.handleBadRequest(new IllegalArgumentException("Email already in use"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getMessage()).contains("Email already in use");
    }

    @Test
    @DisplayName("handleAccessDenied returns 403 with generic message")
    void handleAccessDenied_returns403() {
        ResponseEntity<ErrorResponse> resp = handler.handleAccessDenied(new AccessDeniedException("no access"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().getMessage()).isEqualTo("Access denied");
    }

    @Test
    @DisplayName("handleGeneric returns 500 with a correlation-ID reference, NOT the raw exception message (SEC-M2)")
    void handleGeneric_returns500() {
        ResponseEntity<ErrorResponse> resp = handler.handleGeneric(new RuntimeException("something blew up"));

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(resp.getBody()).isNotNull();
        // Must contain a reference the operator can search in logs
        assertThat(resp.getBody().getMessage()).contains("Support reference:");
        // Must NOT leak the raw exception detail to the caller (SEC-M2)
        assertThat(resp.getBody().getMessage()).doesNotContain("something blew up");
    }
}
