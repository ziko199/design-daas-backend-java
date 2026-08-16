package de.frauas.design.backend.auth;

import de.frauas.design.backend.user.model.BaseUser;
import de.frauas.design.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

/**
 * Single token endpoint at POST /oauth2/user/token.
 * Handles resource-owner password grant and refresh-token grant.
 *
 * <p>Both access tokens and refresh tokens are persisted to the database,
 * matching the PHP reference implementation (oauth2_access_token /
 * oauth2_refresh_tokens tables). Access tokens carry a {@code jti} claim
 * that is checked for revocation by {@link OAuth2SessionController}.</p>
 *
 * <p>Refresh tokens are rotated on every use: the old token is marked revoked
 * and a new one is issued. This enables theft detection per RFC 6749 / RFC 9700.</p>
 */
@RestController
@RequestMapping("/oauth2/user")
@RequiredArgsConstructor
@Slf4j
public class TokenController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenRepository accessTokenRepository;

    /** Cryptographically secure random for refresh token generation. */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /** SEC-H3: lock the account after this many consecutive failed login attempts. */
    private static final int MAX_FAILED_ATTEMPTS = 5;

    /** SEC-H3: duration of the temporary account lock after too many failures. */
    private static final java.time.Duration LOCKOUT_DURATION = java.time.Duration.ofMinutes(15);

    /**
     * SEC-H4: dummy BCrypt hash used when a user is not found so that the
     * response time is indistinguishable from a wrong-password attempt.
     */
    private static final String DUMMY_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOa3G5Wq/8rEjMvT6T3E2z3aJEOv5bIfy";

    @Value("${app.oauth2.issuer:http://localhost:8080}")
    private String issuer;

    @Value("${app.oauth2.access-token-ttl-seconds:3600}")
    private long accessTokenTtlSeconds;

    @Value("${app.oauth2.refresh-token-ttl-days:30}")
    private long refreshTokenTtlDays;

    @PostMapping(value = "/token", consumes = {
            MediaType.APPLICATION_FORM_URLENCODED_VALUE,
            MediaType.APPLICATION_JSON_VALUE
    })
    @Transactional
    public ResponseEntity<?> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam(value = "username",      required = false) String username,
            @RequestParam(value = "password",      required = false) String password,
            @RequestParam(value = "refresh_token", required = false) String refreshToken,
            @RequestParam(value = "scope",         required = false, defaultValue = "") String scope) {

        return switch (grantType) {
            case "password"      -> handlePasswordGrant(username, password, scope);
            case "refresh_token" -> handleRefreshGrant(refreshToken);
            default -> ResponseEntity.badRequest()
                .body(Map.of("error", "unsupported_grant_type",
                             "error_description", "Only 'password' and 'refresh_token' are supported"));
        };
    }

    // -------------------------------------------------------------------------
    // Password grant
    // -------------------------------------------------------------------------

    private ResponseEntity<?> handlePasswordGrant(String username, String password, String scope) {
        if (username == null || password == null) {
            return errorResponse(400, "invalid_request", "username and password are required");
        }

        var userOpt = userRepository.findByEmail(username);
        if (userOpt.isEmpty()) {
            // SEC-H4: always run BCrypt to prevent timing-based user enumeration
            passwordEncoder.matches(password, DUMMY_HASH);
            return errorResponse(401, "invalid_grant", "Invalid credentials");
        }

        BaseUser user = userOpt.get();

        // SEC-H3: reject while the account is locked
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            log.warn("Login attempt on locked account: user={}", username);
            return errorResponse(429, "account_locked",
                    "Account is temporarily locked due to too many failed attempts — try again later");
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            recordFailedAttempt(user);
            return errorResponse(401, "invalid_grant", "Invalid credentials");
        }

        if (!user.isEnabled()) {
            return errorResponse(401, "invalid_grant", "User account is disabled");
        }

        // Successful authentication — reset any lockout state
        if (user.getFailedLoginAttempts() > 0 || user.getLockedUntil() != null) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }

        // SEC-H5: validate requested scopes against what this user role is authorized to receive.
        // Prevents scope-escalation attacks (e.g. a regular user requesting scope=admin).
        Set<String> scopes = resolveAuthorizedScopes(user, scope);
        if (scopes == null) {
            return errorResponse(400, "invalid_scope",
                    "One or more requested scopes are not authorized for this user");
        }

        String accessToken     = issueAccessToken(user, scopes);
        String newRefreshToken = createRefreshToken(user, scopes);

        log.debug("Password grant issued token for user {}", username);
        return tokenResponse(accessToken, newRefreshToken, scopes);
    }

    /**
     * SEC-H3: Increments the failed-login counter and locks the account once
     * {@link #MAX_FAILED_ATTEMPTS} is reached.
     */
    private void recordFailedAttempt(BaseUser user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(LOCKOUT_DURATION));
            log.warn("Account locked after {} failed attempts: user={}", attempts, user.getEmail());
        }
        userRepository.save(user);
    }

    // -------------------------------------------------------------------------
    // Refresh-token grant
    // -------------------------------------------------------------------------

    private ResponseEntity<?> handleRefreshGrant(String refreshToken) {
        if (refreshToken == null) {
            return errorResponse(400, "invalid_request", "refresh_token is required");
        }

        // Only accept non-revoked tokens
        var rtOpt = refreshTokenRepository.findActiveByTokenValue(refreshToken);
        if (rtOpt.isEmpty()) {
            return errorResponse(401, "invalid_grant", "Invalid or already used refresh token");
        }

        RefreshTokenEntity rt = rtOpt.get();
        if (rt.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(rt);
            return errorResponse(401, "invalid_grant", "Refresh token has expired");
        }

        // Re-validate user (mirrors PHP VerifyingRefreshTokenGrant)
        var userOpt = userRepository.findById(rt.getUserId());
        if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
            refreshTokenRepository.delete(rt);
            return errorResponse(401, "invalid_grant", "User not found or disabled");
        }

        BaseUser user    = userOpt.get();
        Set<String> scopes = parsePersistedScopes(rt.getScopes());

        // Issue new access token
        String newAccessToken  = issueAccessToken(user, scopes);

        // Rotate: create new refresh token, then revoke the old one
        String newRefreshToken = createRefreshToken(user, scopes);
        rt.revokeAndReplace(newRefreshToken);
        refreshTokenRepository.save(rt);

        log.debug("Refresh grant rotated token for user {}", user.getEmail());
        return tokenResponse(newAccessToken, newRefreshToken, scopes);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    /**
     * Builds a JWT with a unique {@code jti} claim, persists it to the
     * {@code access_tokens} table, and returns the compact token string.
     */
    private String issueAccessToken(BaseUser user, Set<String> scopes) {
        Instant now  = Instant.now();
        String  jti  = UUID.randomUUID().toString();

        JwtClaimsSet claims = JwtClaimsSet.builder()
            .id(jti)
            .issuer(issuer)
            .subject(String.valueOf(user.getId()))
            .issuedAt(now)
            .expiresAt(now.plusSeconds(accessTokenTtlSeconds))
            .claim("email", user.getEmail())
            .claim("name",  user.getName())
            .claim("scope", String.join(" ", scopes))
            .build();

        JwsHeader header = JwsHeader.with(SignatureAlgorithm.RS256).build();
        String tokenValue = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();

        // Persist for revocation support
        AccessTokenEntity entity = new AccessTokenEntity();
        entity.setJti(jti);
        entity.setUserId(user.getId());
        entity.setScopes(String.join(" ", scopes));
        entity.setIssuedAt(now);
        entity.setExpiresAt(now.plusSeconds(accessTokenTtlSeconds));
        accessTokenRepository.save(entity);

        return tokenValue;
    }

    /**
     * Generates a cryptographically secure refresh token (256-bit hex),
     * persists it, and returns the token string.
     */
    private String createRefreshToken(BaseUser user, Set<String> scopes) {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String tokenValue = HexFormat.of().formatHex(bytes);

        RefreshTokenEntity rt = new RefreshTokenEntity();
        rt.setTokenValue(tokenValue);
        rt.setUserId(user.getId());
        rt.setScopes(String.join(" ", scopes));
        rt.setCreatedAt(Instant.now());
        rt.setExpiresAt(Instant.now().plus(refreshTokenTtlDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(rt);
        return tokenValue;
    }

    /**
     * SEC-H5: Validates the client-requested scope string against the set of scopes
     * that {@code user}'s role is authorized to receive.
     *
     * <ul>
     *   <li>Regular {@code user} → only the {@code user} scope is allowed.</li>
     *   <li>{@code admin} → both {@code admin} and {@code user} scopes are allowed.</li>
     * </ul>
     *
     * <p>If the client requests no scope, the default scope for the user's role is
     * returned. If the client requests a scope outside the authorized set the method
     * returns {@code null} so that the caller can respond with {@code invalid_scope}.</p>
     *
     * <p>The {@code expert} scope (previously registered in the Spring Authorization
     * Server client configuration) is intentionally absent: there is no Expert user
     * type in the domain model, so the scope was dead code and has been removed.</p>
     *
     * @param user          the authenticated user
     * @param requestedScope the raw scope parameter from the token request (may be null/blank)
     * @return the authorized scope set, or {@code null} if any requested scope is forbidden
     */
    private Set<String> resolveAuthorizedScopes(BaseUser user, String requestedScope) {
        Set<String> allowed = allowedScopesFor(user);

        if (requestedScope == null || requestedScope.isBlank()) {
            // Default: grant only the primary scope for this role
            return Set.of(user.getRole());
        }

        Set<String> requested = new HashSet<>(Arrays.asList(requestedScope.trim().split("\\s+")));
        requested.removeIf(String::isBlank);

        // Reject the entire request if any requested scope exceeds the user's authorization
        if (!allowed.containsAll(requested)) {
            log.warn("Scope escalation attempt by user {}: requested={}, allowed={}",
                    user.getEmail(), requested, allowed);
            return null;
        }

        return requested;
    }

    /**
     * Returns the complete set of scopes a user is authorized to hold, derived
     * solely from their discriminator role — not from client input.
     */
    private Set<String> allowedScopesFor(BaseUser user) {
        return switch (user.getRole()) {
            case "admin" -> Set.of("admin", "user");
            case "user"  -> Set.of("user");
            default      -> Set.of("user");
        };
    }

    /**
     * Parses a space-separated scope string that was previously persisted to the
     * database (e.g. the {@code scopes} column of a refresh token). Because these
     * values were already validated at issuance time no role-check is needed here.
     */
    private Set<String> parsePersistedScopes(String scope) {
        if (scope == null || scope.isBlank()) {
            return Set.of("user");
        }
        // SEC-M3: use \\s+ (one-or-more whitespace) — not the character class [\\s+]
        return new HashSet<>(Arrays.asList(scope.trim().split("\\s+")));
    }

    private ResponseEntity<Map<String, Object>> tokenResponse(
            String accessToken, String refreshToken, Set<String> scopes) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("access_token",  accessToken);
        body.put("token_type",    "Bearer");
        body.put("expires_in",    accessTokenTtlSeconds);
        body.put("refresh_token", refreshToken);
        body.put("scope",         String.join(" ", scopes));
        return ResponseEntity.ok(body);
    }

    private ResponseEntity<Map<String, Object>> errorResponse(int status, String error, String description) {
        return ResponseEntity.status(status).body(Map.of(
            "error",             error,
            "error_description", description
        ));
    }
}
