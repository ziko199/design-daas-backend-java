package de.frauas.design.backend.auth.controller;

import de.frauas.design.backend.auth.dto.SessionInfoDto;
import de.frauas.design.backend.auth.exception.SessionException;
import de.frauas.design.backend.auth.exception.SessionExceptionHandler;
import de.frauas.design.backend.auth.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Session-introspection endpoint at {@code GET /oauth2/user/session}, typically used by
 * a frontend to fetch "who am I" details for the currently authenticated user.
 *
 * <p>This controller is intentionally thin: all validation logic (revocation check,
 * user lookup, enabled check, scope parsing) lives in {@link SessionService}, which
 * throws a {@link SessionException} subtype on failure. Those are mapped to HTTP
 * responses by {@link SessionExceptionHandler}, not here, so this class has no
 * knowledge of error-response shapes at all.</p>
 */
@Slf4j
@RestController
@RequiredArgsConstructor
public class OAuth2SessionController {

    private final SessionService sessionService;

    @GetMapping("/oauth2/user/session")
    public ResponseEntity<SessionInfoDto> session(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            log.warn("GET /oauth2/user/session — no JWT present, returning 401");
            return ResponseEntity.status(401).build();
        }

        log.info("GET /oauth2/user/session — checking session for sub={}", jwt.getSubject());
        SessionInfoDto session = sessionService.getSession(jwt);
        return ResponseEntity.ok(session);
    }
}
