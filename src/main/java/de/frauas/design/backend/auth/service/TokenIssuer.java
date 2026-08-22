package de.frauas.design.backend.auth.service;

import de.frauas.design.backend.auth.model.AccessTokenEntity;
import de.frauas.design.backend.auth.model.RefreshTokenEntity;
import de.frauas.design.backend.auth.repository.AccessTokenRepository;
import de.frauas.design.backend.auth.repository.RefreshTokenRepository;
import de.frauas.design.backend.user.model.BaseUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

/**
 * Builds and persists access tokens (signed JWTs) and refresh tokens (opaque
 * random strings), matching the PHP reference implementation's
 * {@code oauth2_access_token} / {@code oauth2_refresh_tokens} tables.
 * Extracted from {@link TokenService} so token-issuance mechanics can be
 * unit-tested independently of grant handling.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TokenIssuer {

    /**
     * Cryptographically secure random for refresh token generation.
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JwtEncoder jwtEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenRepository accessTokenRepository;

    @Value("${app.oauth2.issuer:http://localhost:8080}")
    private String issuer;

    @Value("${app.oauth2.access-token-ttl-seconds:3600}")
    private long accessTokenTtlSeconds;

    @Value("${app.oauth2.refresh-token-ttl-days:30}")
    private long refreshTokenTtlDays;

    /**
     * The configured access-token lifetime, in seconds, as reported to clients
     * via the {@code expires_in} field.
     */
    public long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    /**
     * Builds a JWT with a unique {@code jti} claim, persists it to the
     * {@code access_tokens} table, and returns the compact token string.
     */
    public String issueAccessToken(BaseUser user, Set<String> scopes) {
        Instant now = Instant.now();
        String jti = UUID.randomUUID().toString();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .id(jti)
                .issuer(issuer)
                .subject(String.valueOf(user.getId()))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(accessTokenTtlSeconds))
                .claim("email", user.getEmail())
                .claim("name", user.getName())
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
        log.debug("issueAccessToken — issued jti={} userId={}", jti, user.getId());

        return tokenValue;
    }

    /**
     * Generates a cryptographically secure refresh token (256-bit hex),
     * persists it, and returns the token string.
     */
    public String createRefreshToken(BaseUser user, Set<String> scopes) {
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
        log.debug("createRefreshToken — issued userId={}", user.getId());
        return tokenValue;
    }
}
