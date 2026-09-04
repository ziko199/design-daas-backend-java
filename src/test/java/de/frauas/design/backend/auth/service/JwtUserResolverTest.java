package de.frauas.design.backend.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtUserResolver")
class JwtUserResolverTest {

    @Mock
    JwtDecoder jwtDecoder;

    JwtUserResolver jwtUserResolver;

    @BeforeEach
    void setUp() {
        jwtUserResolver = new JwtUserResolver(jwtDecoder);
    }

    @Test
    @DisplayName("resolveFromRawToken returns null for null or blank tokens")
    void resolveFromRawToken_nullOrBlank_returnsNull() {
        assertThat(jwtUserResolver.resolveFromRawToken(null)).isNull();
        assertThat(jwtUserResolver.resolveFromRawToken("   ")).isNull();
        verifyNoInteractions(jwtDecoder);
    }

    @Test
    @DisplayName("resolveFromRawToken returns null when decoding fails")
    void resolveFromRawToken_invalidToken_returnsNull() {
        when(jwtDecoder.decode("bad-token")).thenThrow(new JwtException("bad token"));

        assertThat(jwtUserResolver.resolveFromRawToken("bad-token")).isNull();
    }

    @Test
    @DisplayName("resolveFromRawToken returns null for non-numeric subjects")
    void resolveFromRawToken_nonNumericSubject_returnsNull() {
        when(jwtDecoder.decode("token")).thenReturn(jwt("abc"));

        assertThat(jwtUserResolver.resolveFromRawToken("token")).isNull();
    }

    @Test
    @DisplayName("resolveFromRawToken returns the numeric subject")
    void resolveFromRawToken_validToken_returnsUserId() {
        when(jwtDecoder.decode("token")).thenReturn(jwt("42"));

        assertThat(jwtUserResolver.resolveFromRawToken("token")).isEqualTo(42);
    }

    @Test
    @DisplayName("resolveFromAuthenticatedJwt returns null for null or non-numeric subjects")
    void resolveFromAuthenticatedJwt_nullOrNonNumeric_returnsNull() {
        assertThat(jwtUserResolver.resolveFromAuthenticatedJwt(null)).isNull();
        assertThat(jwtUserResolver.resolveFromAuthenticatedJwt(jwt("not-a-number")))
                .isNull();
    }

    @Test
    @DisplayName("resolveFromAuthenticatedJwt returns the numeric subject")
    void resolveFromAuthenticatedJwt_validJwt_returnsUserId() {
        assertThat(jwtUserResolver.resolveFromAuthenticatedJwt(jwt("7"))).isEqualTo(7);
    }

    private Jwt jwt(String subject) {
        return Jwt.withTokenValue("token-value")
                .header("alg", "RS256")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
    }
}
