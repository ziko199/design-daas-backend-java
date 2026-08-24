package de.frauas.design.backend.permission.controller;

import de.frauas.design.backend.auth.service.JwtUserResolver;
import de.frauas.design.backend.permission.dto.PermissionResultDto;
import de.frauas.design.backend.permission.service.PermissionService;
import de.frauas.design.backend.security.Authorities;
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
@PreAuthorize(Authorities.IS_ADMIN)
public class PermissionController {

    private final PermissionService permissionService;
    private final JwtUserResolver jwtUserResolver;

    /**
     * GET /permissions/{functionName}/{userId}
     * Called by design-daas with an admin Bearer token to check if a specific
     * user is allowed to call the given function.
     */
    @GetMapping("/permissions/{functionName}/{userId}")
    public ResponseEntity<PermissionResultDto> checkPermission(
            @PathVariable String functionName, @PathVariable Integer userId) {

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
     * <p>{@code function_name} is validated before use — a missing or blank
     * value previously caused a {@code NullPointerException} at the JPA layer that
     * leaked through {@code GlobalExceptionHandler} as a 500 with internal details.</p>
     */
    @PostMapping("/permissions_info")
    public ResponseEntity<PermissionResultDto> permissionsInfo(
            @RequestBody Map<String, String> body, @AuthenticationPrincipal Jwt authJwt) {

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
     *
     * @param bodyToken the raw JWT string from the request body (may be null)
     * @param authJwt   the JWT from the Authorization header (may be null)
     * @return the user ID, or null if no valid token is available
     */
    private Integer resolveUserId(String bodyToken, Jwt authJwt) {
        if (bodyToken != null && !bodyToken.isBlank()) {
            return jwtUserResolver.resolveFromRawToken(bodyToken);
        }
        return jwtUserResolver.resolveFromAuthenticatedJwt(authJwt);
    }
}
