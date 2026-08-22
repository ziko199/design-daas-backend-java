package de.frauas.design.backend.auth.service;

import de.frauas.design.backend.auth.model.AccessTokenEntity;
import de.frauas.design.backend.auth.repository.AccessTokenRepository;
import de.frauas.design.backend.user.model.BaseUser;
import de.frauas.design.backend.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Rejects JWTs whose access token has been revoked, or whose underlying user no longer
 * exists / is disabled — enforced for <strong>every</strong> authenticated request via the
 * resource-server filter chain (wired in {@code AuthorizationServerConfig#jwtDecoder}).
 *
 * <p>Before this validator existed, the {@code access_tokens.revoked} flag and the
 * {@code enabled} flag on {@code BaseUser} were only checked manually inside
 * {@link SessionService} for the single {@code GET /oauth2/user/session} endpoint —
 * every other protected endpoint (e.g. {@code /users}, {@code /admin/**}) accepted any
 * signed, non-expired JWT regardless of revocation or account status. Disabling or
 * locking a user therefore had no effect on their already-issued access tokens until
 * natural expiry (default 1 hour). This validator closes that gap by running as part of
 * standard JWT validation (alongside the default expiry/issued-at checks) for every
 * request that goes through {@code oauth2ResourceServer().jwt(...)}.</p>
 */
@Slf4j
public class TokenRevocationValidator implements OAuth2TokenValidator<Jwt> {

    private static final OAuth2Error REVOKED_ERROR = new OAuth2Error(
            OAuth2ErrorCodes.INVALID_TOKEN, "The access token has been revoked or is unknown", null);

    private static final OAuth2Error DISABLED_ERROR = new OAuth2Error(
            OAuth2ErrorCodes.INVALID_TOKEN, "The token's user account no longer exists or is disabled", null);

    private final AccessTokenRepository accessTokenRepository;
    private final UserRepository userRepository;

    public TokenRevocationValidator(AccessTokenRepository accessTokenRepository, UserRepository userRepository) {
        this.accessTokenRepository = accessTokenRepository;
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        String jti = token.getId();
        if (jti == null) {
            log.warn("TokenRevocationValidator — token has no jti claim, rejecting");
            return OAuth2TokenValidatorResult.failure(REVOKED_ERROR);
        }

        boolean revoked = accessTokenRepository.findByJti(jti)
                .map(AccessTokenEntity::isRevoked)
                .orElse(true);
        if (revoked) {
            log.warn("TokenRevocationValidator — rejected revoked/unknown jti={}", jti);
            return OAuth2TokenValidatorResult.failure(REVOKED_ERROR);
        }

        Integer userId;
        try {
            userId = Integer.parseInt(token.getSubject());
        } catch (NumberFormatException e) {
            log.warn("TokenRevocationValidator — non-numeric subject={}", token.getSubject());
            return OAuth2TokenValidatorResult.failure(DISABLED_ERROR);
        }

        BaseUser user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isEnabled()) {
            log.warn("TokenRevocationValidator — rejected disabled/missing user userId={}", userId);
            return OAuth2TokenValidatorResult.failure(DISABLED_ERROR);
        }

        return OAuth2TokenValidatorResult.success();
    }
}
