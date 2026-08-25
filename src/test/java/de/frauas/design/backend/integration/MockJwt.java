package de.frauas.design.backend.integration;

import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.UUID;

/**
 * Shared helpers for building mock JWTs in integration tests.
 * Uses Spring Security Test's jwt() post-processor so no real RSA key is needed.
 *
 * <p>A unique {@code jti} is included so that the OAuth2SessionController's
 * revocation check does not reject these tokens (unknown jti → revoked).</p>
 */
public final class MockJwt {

    private MockJwt() {}

    /** Creates a mock JWT for a user-role principal with the given userId. */
    public static RequestPostProcessor userJwt(Integer userId) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.jti(UUID.randomUUID().toString())
                        .subject(String.valueOf(userId))
                        .claim("scope", "user")
                        .claim("email", "user" + userId + "@test.com")
                        .claim("name", "Test User " + userId)
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(3600)));
    }

    /** Creates a mock JWT for an admin-role principal. */
    public static RequestPostProcessor adminJwt(Integer userId) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.jti(UUID.randomUUID().toString())
                        .subject(String.valueOf(userId))
                        .claim("scope", "admin")
                        .claim("email", "admin" + userId + "@test.com")
                        .claim("name", "Admin " + userId)
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(3600)))
                .authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority("SCOPE_admin"));
    }
}
