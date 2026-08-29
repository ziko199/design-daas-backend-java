package de.frauas.design.backend.permission.controller;

import de.frauas.design.backend.auth.service.JwtUserResolver;
import de.frauas.design.backend.permission.dto.PermissionResultDto;
import de.frauas.design.backend.permission.service.PermissionService;
import de.frauas.design.backend.shared.security.Authorities;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Exposes endpoints used by design-daas to check whether a given user is allowed
 * to invoke a given function.
 *
 * <p>All endpoints require an admin-scoped bearer token ({@code SCOPE_admin}), enforced
 * once at class level via {@link PreAuthorize} since every handler shares the same
 * requirement.</p>
 */
@RestController
@RequiredArgsConstructor
@Slf4j
public class PermissionController {

    private static final int UNRESOLVED_USER_ID = 0;
    private static final String UNKNOWN_USER_NAME = "unknown";

    private final PermissionService permissionService;
    private final JwtUserResolver jwtUserResolver;

    /**
     * Checks whether the given user may invoke the given function.
     *
     * @param functionName the design-daas function to check
     * @param userId the target user ID
     * @return an allow/deny payload for the requested user/function pair
     */
    @PreAuthorize(Authorities.IS_ADMIN)
    @GetMapping("/permissions/{functionName}/{userId}")
    public ResponseEntity<PermissionResultDto> checkPermission(
            @PathVariable String functionName, @PathVariable Integer userId) {

        log.info("GET /permissions/{}/{} — checking permission", functionName, userId);

        PermissionResultDto result = permissionService.checkPermission(userId, functionName);

        log.debug("GET /permissions/{}/{} — result={}", functionName, userId, result.getResult());

        return ResponseEntity.ok(result);
    }

    /**
     * Returns the caller's permission result for the requested function.
     *
     * <p>If the body contains a raw {@code token}, that token is validated and used as the identity
     * source. Otherwise, the already-authenticated {@code Authorization} header JWT is used.</p>
     *
     * <p>{@code function_name} is validated before use — a missing or blank value previously caused
     * a {@code NullPointerException} at the JPA layer that leaked through
     * {@code GlobalExceptionHandler} as a 500 with internal details.</p>
     *
     * @param body request payload containing {@code function_name} and optionally {@code token};
     *             may be {@code null}
     * @param authJwt authenticated caller JWT, used as a fallback identity source
     * @return {@code 400 Bad Request} for a missing/blank function name, otherwise an allow/deny
     *         permission payload
     */
    @PostMapping("/permissions_info")
    public ResponseEntity<PermissionResultDto> permissionsInfo(
            @RequestBody Map<String, String> body, @AuthenticationPrincipal Jwt authJwt) {

        String functionName = body != null ? body.get("function_name") : null;

        log.info("POST /permissions_info — functionName={}", functionName);

        if (functionName == null || functionName.isBlank()) {
            log.warn("POST /permissions_info — missing function_name");
            return ResponseEntity.badRequest().build();
        }

        Integer userId = resolveUserId(body.get("token"), authJwt);

        if (userId == null) {
            log.warn("POST /permissions_info — could not resolve user from token, returning deny");
            return ResponseEntity.ok(PermissionResultDto.deny(UNRESOLVED_USER_ID, UNKNOWN_USER_NAME));
        }

        PermissionResultDto result = permissionService.checkPermission(userId, functionName);

        log.debug("POST /permissions_info — userId={} function={} result={}", userId, functionName, result.getResult());

        return ResponseEntity.ok(result);
    }

    /**
     * Resolves the user ID from either the body-supplied token or the Authorization header JWT.
     *
     * @param bodyToken the raw JWT string from the request body (maybe null)
     * @param authJwt the JWT from the Authorization header (maybe null)
     * @return the user ID, or null if no valid token is available
     */
    private Integer resolveUserId(String bodyToken, Jwt authJwt) {
        if (bodyToken != null && !bodyToken.isBlank()) {
            return jwtUserResolver.resolveFromRawToken(bodyToken);
        }
        return jwtUserResolver.resolveFromAuthenticatedJwt(authJwt);
    }
}
