package de.frauas.design.backend.auth;

import de.frauas.design.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
public class OAuth2SessionController {

    private final UserRepository userRepository;
    private final AccessTokenRepository accessTokenRepository;

    @GetMapping("/oauth2/user/session")
    public ResponseEntity<?> session(@AuthenticationPrincipal Jwt jwt) {
        if (jwt == null) {
            log.warn("GET /oauth2/user/session — no JWT present, returning 401");
            return ResponseEntity.status(401).build();
        }

        String jti = jwt.getId();
        log.debug("GET /oauth2/user/session — validating token jti={} sub={}", jti, jwt.getSubject());

        if (jti != null) {
            boolean revoked = accessTokenRepository.findByJti(jti)
                    .map(AccessTokenEntity::isRevoked)
                    .orElse(true);
            if (revoked) {
                log.warn("GET /oauth2/user/session — token revoked jti={}", jti);
                return ResponseEntity.status(401)
                        .body(Map.of("error", "token_revoked",
                                     "error_description", "The access token has been revoked"));
            }
        }

        Integer userId = Integer.parseInt(jwt.getSubject());

        var user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found for id: " + userId));

        if (!user.isEnabled()) {
            log.warn("GET /oauth2/user/session — user disabled userId={}", userId);
            return ResponseEntity.status(401)
                    .body(Map.of("error", "user_disabled",
                                 "error_description", "User account is disabled"));
        }

        List<String> scopes;
        Object scopeClaim = jwt.getClaim("scope");
        if (scopeClaim instanceof String s && !s.isBlank()) {
            scopes = List.of(s.split("\\s+"));
        } else if (scopeClaim instanceof List<?> list) {
            scopes = list.stream().map(Object::toString).toList();
        } else {
            scopes = List.of();
        }

        log.info("GET /oauth2/user/session — session valid userId={} role={} scopes={}", userId, user.getRole(), scopes);

        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("id",      userId);
        body.put("user_id", userId);
        body.put("name",    user.getName());
        body.put("email",   user.getEmail());
        body.put("role",    user.getRole());
        body.put("scopes",  scopes);
        body.put("enabled", user.isEnabled());
        return ResponseEntity.ok(body);
    }
}
