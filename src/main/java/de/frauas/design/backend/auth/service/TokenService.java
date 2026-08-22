package de.frauas.design.backend.auth.service;

import de.frauas.design.backend.auth.dto.TokenResponseDto;
import de.frauas.design.backend.auth.exception.AccountDisabledException;
import de.frauas.design.backend.auth.exception.ExpiredRefreshTokenException;
import de.frauas.design.backend.auth.exception.InvalidCredentialsException;
import de.frauas.design.backend.auth.exception.InvalidRefreshTokenException;
import de.frauas.design.backend.auth.exception.InvalidRequestException;
import de.frauas.design.backend.auth.exception.RefreshTokenUserInvalidException;
import de.frauas.design.backend.auth.exception.UnsupportedGrantTypeException;
import de.frauas.design.backend.auth.model.RefreshTokenEntity;
import de.frauas.design.backend.auth.repository.RefreshTokenRepository;
import de.frauas.design.backend.user.model.BaseUser;
import de.frauas.design.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Core business logic for the OAuth2 resource-owner password grant and refresh-token
 * grant, extracted from {@code TokenController} so that lockout, scope-escalation, and
 * token-issuance/rotation rules can be unit-tested independently of the web layer.
 *
 * <p>Both access tokens and refresh tokens are persisted to the database, matching the
 * PHP reference implementation ({@code oauth2_access_token} / {@code oauth2_refresh_tokens}
 * tables). Access tokens carry a {@code jti} claim that is checked for revocation by
 * {@link TokenRevocationValidator} on every authenticated request, and by
 * {@link SessionService} for the {@code /oauth2/user/session} endpoint.</p>
 *
 * <p>Refresh tokens are rotated on every use: the old token is marked revoked and a new
 * one is issued. This enables theft detection per RFC 6749 / RFC 9700.</p>
 *
 * <p>This class focuses solely on orchestrating the two grants; account lockout,
 * scope resolution, and token issuance are delegated to {@link AccountLockoutService},
 * {@link ScopeResolver}, and {@link TokenIssuer} respectively.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    /**
     * dummy BCrypt hash used when a user is not found so that the
     * response time is indistinguishable from a wrong-password attempt.
     */
    private static final String DUMMY_HASH =
            "$2a$10$7EqJtq98hPqEX7fNZaFWoOa3G5Wq/8rEjMvT6T3E2z3aJEOv5bIfy";
    /**
     * token type advertised to clients, per RFC 6749 §5.1.
     */
    private static final String BEARER_TOKEN_TYPE = "Bearer";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccountLockoutService accountLockoutService;
    private final ScopeResolver scopeResolver;
    private final TokenIssuer tokenIssuer;

    // -------------------------------------------------------------------------
    // Grant dispatch
    // -------------------------------------------------------------------------

    /**
     * Dispatches to the appropriate grant handler based on {@code grant_type},
     * as required by RFC 6749. Extracted from {@code TokenController} so that
     * the controller stays a thin HTTP adapter and this routing is unit-testable
     * alongside the rest of the grant logic.
     *
     * @param grantType    {@code "password"} or {@code "refresh_token"}
     * @param username     resource-owner email; used only for the password grant
     * @param password     resource-owner password; used only for the password grant
     * @param refreshToken the refresh token to redeem; used only for the refresh-token grant
     * @param scope        client-requested scope string; used only for the password grant
     * @return the newly issued access/refresh token pair
     * @throws UnsupportedGrantTypeException if {@code grantType} is not recognized
     */
    public TokenResponseDto grantToken(String grantType, String username, String password,
                                           String refreshToken, String scope) {
        return switch (grantType) {
            case "password" -> passwordGrant(username, password, scope);
            case "refresh_token" -> refreshGrant(refreshToken);
            default -> throw new UnsupportedGrantTypeException();
        };
    }

    // -------------------------------------------------------------------------
    // Password grant
    // -------------------------------------------------------------------------

    /**
     * Handles the resource-owner password grant: validates credentials, enforces
     * account lockout and timing-attack mitigation, validates the
     * requested scope, and issues a new access/refresh token pair.
     */
    @Transactional
    public TokenResponseDto passwordGrant(String username, String password, String scope) {
        log.debug("passwordGrant — username={}", username);
        if (username == null || password == null) {
            log.warn("passwordGrant — missing username or password");
            throw new InvalidRequestException("username and password are required");
        }

        var userOpt = userRepository.findByEmail(username);
        if (userOpt.isEmpty()) {
            // always run BCrypt to prevent timing-based user enumeration
            passwordEncoder.matches(password, DUMMY_HASH);
            log.warn("passwordGrant — unknown user={}", username);
            throw new InvalidCredentialsException();
        }

        BaseUser user = userOpt.get();

        // reject while the account is locked
        accountLockoutService.assertNotLocked(user);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            accountLockoutService.recordFailedAttempt(user);
            log.warn("passwordGrant — wrong password user={}", username);
            throw new InvalidCredentialsException();
        }

        if (!user.isEnabled()) {
            log.warn("passwordGrant — disabled account user={}", username);
            throw new AccountDisabledException();
        }

        // Successful authentication — reset any lockout state
        accountLockoutService.resetOnSuccess(user);

        // validate requested scope against what this user role is authorized to receive.
        // Prevents scope-escalation attacks (e.g. a regular user requesting scope=admin).
        String grantedScope = scopeResolver.resolveAuthorizedScope(user, scope);

        String accessToken = tokenIssuer.issueAccessToken(user, grantedScope);
        String newRefreshToken = tokenIssuer.createRefreshToken(user, grantedScope);

        log.info("passwordGrant — issued token for user={} userId={} scope={}", username, user.getId(), grantedScope);
        return new TokenResponseDto(accessToken, BEARER_TOKEN_TYPE, newRefreshToken, grantedScope,
                tokenIssuer.getAccessTokenTtlSeconds());
    }

    // -------------------------------------------------------------------------
    // Refresh-token grant
    // -------------------------------------------------------------------------

    /**
     * Handles the refresh-token grant: validates and rotates the refresh token,
     * re-validates the underlying user, and issues a new access/refresh token pair.
     */
    @Transactional
    public TokenResponseDto refreshGrant(String refreshToken) {
        log.debug("refreshGrant — token=[REDACTED]");
        if (refreshToken == null) {
            log.warn("refreshGrant — missing refresh_token");
            throw new InvalidRequestException("refresh_token is required");
        }

        // Only accept non-revoked tokens
        var refreshTokenOpt = refreshTokenRepository.findActiveByTokenValue(refreshToken);
        if (refreshTokenOpt.isEmpty()) {
            log.warn("refreshGrant — invalid or already-used refresh token");
            throw new InvalidRefreshTokenException();
        }

        RefreshTokenEntity refreshTokenEntity = refreshTokenOpt.get();
        if (refreshTokenEntity.getExpiresAt().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshTokenEntity);
            log.warn("refreshGrant — expired refresh token userId={}", refreshTokenEntity.getUserId());
            throw new ExpiredRefreshTokenException();
        }

        // Re-validate user (mirrors PHP VerifyingRefreshTokenGrant)
        var userOpt = userRepository.findById(refreshTokenEntity.getUserId());
        if (userOpt.isEmpty() || !userOpt.get().isEnabled()) {
            refreshTokenRepository.delete(refreshTokenEntity);
            log.warn("refreshGrant — user not found or disabled userId={}", refreshTokenEntity.getUserId());
            throw new RefreshTokenUserInvalidException();
        }

        BaseUser user = userOpt.get();
        String grantedScope = scopeResolver.parsePersistedScope(refreshTokenEntity.getScope());

        // Issue new access token
        String newAccessToken = tokenIssuer.issueAccessToken(user, grantedScope);

        // Rotate: create new refresh token, then revoke the old one
        String newRefreshToken = tokenIssuer.createRefreshToken(user, grantedScope);
        refreshTokenEntity.revoke();
        refreshTokenRepository.save(refreshTokenEntity);

        log.info("refreshGrant — rotated token for user={} userId={}", user.getEmail(), user.getId());
        return new TokenResponseDto(newAccessToken, BEARER_TOKEN_TYPE, newRefreshToken, grantedScope,
                tokenIssuer.getAccessTokenTtlSeconds());
    }
}
