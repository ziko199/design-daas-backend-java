package de.frauas.design.backend.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

/**
 * Resolves the numeric user ID behind a JWT — either decoded fresh from a raw token
 * string (e.g. supplied in a request body) or already authenticated via the
 * {@code Authorization} header.
 *
 * <p>Token validity (expiry, issuer, and revocation) is enforced by the {@link JwtDecoder}
 * bean itself — see {@code AuthorizationServerConfig#jwtDecoder}, which wires in
 * {@link TokenRevocationValidator}. This class only needs to decode and read the
 * {@code sub} claim; it does not duplicate any of that validation.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUserResolver {

    private final JwtDecoder jwtDecoder;

    /**
     * Decodes and validates a raw JWT string (expiry, issuer, revocation) and returns
     * the user ID from its subject claim.
     *
     * @param rawToken the raw JWT string, may be null/blank
     * @return the user ID, or {@code null} if the token is missing, invalid, expired,
     *         revoked, or has a non-numeric subject
     */
    public Integer resolveFromRawToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return null;
        }
        try {
            Jwt decoded = jwtDecoder.decode(rawToken);
            return parseSubject(decoded);
        } catch (JwtException e) {
            log.debug("JwtUserResolver — raw token invalid: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Reads the user ID from a JWT that Spring Security has already authenticated
     * (e.g. via {@code @AuthenticationPrincipal}). No further validation is performed.
     *
     * @param jwt the authenticated JWT, may be null
     * @return the user ID, or {@code null} if absent or the subject is non-numeric
     */
    public Integer resolveFromAuthenticatedJwt(Jwt jwt) {
        return jwt == null ? null : parseSubject(jwt);
    }

    private Integer parseSubject(Jwt jwt) {
        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(sub);
        } catch (NumberFormatException e) {
            log.debug("JwtUserResolver — non-numeric subject: {}", sub);
            return null;
        }
    }
}
