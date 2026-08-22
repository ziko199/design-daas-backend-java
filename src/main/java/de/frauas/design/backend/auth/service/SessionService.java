package de.frauas.design.backend.auth.service;

import de.frauas.design.backend.auth.dto.SessionInfoDto;
import de.frauas.design.backend.auth.exception.SessionException;
import de.frauas.design.backend.auth.exception.TokenRevokedException;
import de.frauas.design.backend.auth.exception.UserDisabledException;
import de.frauas.design.backend.auth.exception.UserNotFoundException;
import de.frauas.design.backend.auth.model.AccessTokenEntity;
import de.frauas.design.backend.auth.repository.AccessTokenRepository;
import de.frauas.design.backend.user.model.BaseUser;
import de.frauas.design.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Business logic behind {@code GET /oauth2/user/session}: validates that the presented
 * JWT's access token has not been revoked and that the underlying user still exists and
 * is enabled, then returns a summary of the session.
 *
 * <p>Extracted from {@code OAuth2SessionController} so the validation rules can be
 * unit-tested without a Spring context.</p>
 *
 * <p><b>Failure handling:</b> an invalid session (revoked token, unknown/disabled user)
 * is an expected outcome of calling this endpoint, not a bug — so each reason is signalled
 * via its own unchecked {@link SessionException} subtype ({@link TokenRevokedException},
 * {@link UserNotFoundException}, {@link UserDisabledException}) rather than a generic
 * exception. {@code SessionExceptionHandler} catches the common {@link SessionException}
 * base type and maps it to the right HTTP status/body, while anything else (a real bug)
 * still falls through to {@code GlobalExceptionHandler} as a 500. This keeps the "happy
 * path" return type simple ({@link SessionInfoDto}) while still making every failure case
 * explicit and testable.</p>
 *
 * <p>Note: this is a defense-in-depth / introspection endpoint. The authoritative
 * revocation check for every other authenticated endpoint is {@code TokenRevocationValidator},
 * which runs as part of the JWT validation in the resource-server filter chain (see
 * {@code AuthorizationServerConfig#jwtDecoder}). This service duplicates that check so a
 * caller of this specific endpoint gets a descriptive error body (built by
 * {@code SessionExceptionHandler}), but the request would already have been rejected
 * earlier if the validator considered the token invalid.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SessionService {

    private final UserRepository userRepository;
    private final AccessTokenRepository accessTokenRepository;

    /**
     * Validates the given JWT and returns the session summary.
     *
     * @throws TokenRevokedException if the token is unknown or revoked
     * @throws UserNotFoundException if the token's user no longer exists
     * @throws UserDisabledException if the token's user is disabled
     */
    public SessionInfoDto getSession(Jwt jwt) {
        String jti = jwt.getId();
        log.debug("getSession — validating token jti={} sub={}", jti, jwt.getSubject());

        // A missing jti is treated the same as an unknown/revoked token — every token issued
        // by TokenService always carries one, so its absence indicates a malformed or foreign token.
        boolean revoked = jti == null || accessTokenRepository.findByJti(jti)
                .map(AccessTokenEntity::isRevoked)
                .orElse(true);
        if (revoked) {
            log.warn("getSession — token revoked or unknown jti={}", jti);
            throw new TokenRevokedException();
        }

        Integer userId = Integer.parseInt(Objects.requireNonNull(jwt.getSubject()));
        BaseUser user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("getSession — no user found for token subject userId={}", userId);
                    return new UserNotFoundException();
                });

        if (!user.isEnabled()) {
            log.warn("getSession — user disabled userId={}", userId);
            throw new UserDisabledException();
        }

        List<String> scopes = parseScopeClaim(jwt);
        log.info("getSession — session valid userId={} role={} scopes={}", userId, user.getRole(), scopes);
        return new SessionInfoDto(userId, user.getName());
    }

    /** Parses the JWT's {@code scope} claim, which may be a space-delimited string or a list. */
    private List<String> parseScopeClaim(Jwt jwt) {
        Object scopeClaim = jwt.getClaim("scope");
        if (scopeClaim instanceof String s && !s.isBlank()) {
            return List.of(s.split("\\s+"));
        }
        if (scopeClaim instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of();
    }
}
