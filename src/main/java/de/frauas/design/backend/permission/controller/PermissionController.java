package de.frauas.design.backend.permission.controller;

import de.frauas.design.backend.auth.AccessTokenRepository;
import de.frauas.design.backend.permission.dto.PermissionResultDto;
import de.frauas.design.backend.permission.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
public class PermissionController {

    private final PermissionService permissionService;
    private final JwtDecoder jwtDecoder;
    private final AccessTokenRepository accessTokenRepository;

    /**
     * GET /permissions/{functionName}/{userId}
     * Called by design-daas with an admin Bearer token to check if a specific
     * user is allowed to call the given function.
     */
    @GetMapping("/permissions/{functionName}/{userId}")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ResponseEntity<PermissionResultDto> checkPermission(
            @PathVariable String functionName,
            @PathVariable Integer userId) {
        log.info("GET /permissions/{}/{} — checking permission", functionName, userId);
        PermissionResultDto result = permissionService.checkPermission(userId, functionName);
        log.debug("GET /permissions/{}/{} — result={}", functionName, userId, result.getResult());
        return ResponseEntity.ok(result);
    }

    /**
     * POST /permissions_info
     * Validates the token supplied in the request body (field {@code token}) and returns
     * the permission result for the token's user on the given {@code function_name}.
     *
     * <p>Mirrors PHP PermissionsHttpController::permissionInfoAction() which reads
     * {@code token} and {@code function_name} from the request body, validates the token
     * independently via OAuth2AuthorizationValidator::tokenInfo(), and then calls
     * PermissionCalculationService::getPermission().</p>
     *
     * <p>If no {@code token} field is present in the body, falls back to the JWT from
     * the Authorization header (for backward compatibility with admin callers).</p>
     *
     * <p>SEC-M5: {@code function_name} is validated before use — a missing or blank
     * value previously caused a {@code NullPointerException} at the JPA layer that
     * leaked through {@code GlobalExceptionHandler} as a 500 with internal details.</p>
     */
    @PostMapping("/permissions_info")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ResponseEntity<PermissionResultDto> permissionsInfo(
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt authJwt) {
        String functionName = body.get("function_name");
        log.info("POST /permissions_info — functionName={}", functionName);
        if (functionName == null || functionName.isBlank()) {
            log.warn("POST /permissions_info — missing function_name");
            return ResponseEntity.badRequest().build();
        }

        Integer userId = resolveUserId(body.get("token"), authJwt);
        if (userId == null) {
            log.warn("POST /permissions_info — could not resolve user from token, returning deny");
            return ResponseEntity.ok(PermissionResultDto.deny(0, "unknown"));
        }

        PermissionResultDto result = permissionService.checkPermission(userId, functionName);
        log.debug("POST /permissions_info — userId={} function={} result={}", userId, functionName, result.getResult());
        return ResponseEntity.ok(result);
    }

    /**
     * Resolves the user ID from either the body-supplied token or the Authorization header JWT.
     * Mirrors PHP OAuth2AuthorizationValidator::tokenInfo().
     *
     * @param bodyToken the raw JWT string from the request body (may be null)
     * @param authJwt   the JWT from the Authorization header (may be null)
     * @return the user ID, or null if no valid token is available
     */
    private Integer resolveUserId(String bodyToken, Jwt authJwt) {
        if (bodyToken != null && !bodyToken.isBlank()) {
            try {
                Jwt decoded = jwtDecoder.decode(bodyToken);
                // Check revocation
                String jti = decoded.getId();
                if (jti != null) {
                    boolean revoked = accessTokenRepository.findByJti(jti)
                        .map(at -> at.isRevoked())
                        .orElse(true);
                    if (revoked) {
                        log.debug("permissions_info: body token with jti={} is revoked", jti);
                        return null;
                    }
                }
                String sub = decoded.getSubject();
                if (sub != null && !sub.isBlank()) {
                    return Integer.parseInt(sub);
                }
            } catch (JwtException | NumberFormatException e) {
                log.debug("permissions_info: body token invalid — {}", e.getMessage());
                return null;
            }
        }
        // Fall back to Authorization header token
        if (authJwt != null && authJwt.getSubject() != null) {
            return Integer.parseInt(authJwt.getSubject());
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Debug / ping endpoints (mirrors PHP OAuth2HttpController)
    // -------------------------------------------------------------------------

    /**
     * GET /ping/user — returns 200 only when caller has the 'user' scope.
     * Used by design-daas to verify user-scoped tokens.
     */
    @GetMapping("/ping/user")
    @PreAuthorize("hasAuthority('SCOPE_user')")
    public ResponseEntity<Void> pingUser() {
        log.debug("GET /ping/user — OK");
        return ResponseEntity.ok().build();
    }

    @GetMapping("/ping/admin")
    @PreAuthorize("hasAuthority('SCOPE_admin')")
    public ResponseEntity<Void> pingAdmin() {
        log.debug("GET /ping/admin — OK");
        return ResponseEntity.ok().build();
    }
}
