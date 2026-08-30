package de.frauas.design.backend.auth.service;

import de.frauas.design.backend.auth.model.AccessTokenEntity;
import de.frauas.design.backend.auth.repository.AccessTokenRepository;
import de.frauas.design.backend.user.model.User;
import de.frauas.design.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TokenRevocationValidator")
class TokenRevocationValidatorTest {

    @Mock
    AccessTokenRepository accessTokenRepository;

    @Mock
    UserRepository userRepository;

    TokenRevocationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new TokenRevocationValidator(accessTokenRepository, userRepository);
    }

    private Jwt jwt(String jti, Integer subject) {
        Jwt.Builder builder = Jwt.withTokenValue("token-value")
                .header("alg", "RS256")
                .subject(String.valueOf(subject))
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600));
        if (jti != null) {
            builder.jti(jti);
        } else {
            builder.claim("placeholder", "x");
        }
        return builder.build();
    }

    private User enabledUser(Integer id) {
        User u = new User();
        u.setId(id);
        u.setEmail("u" + id + "@example.com");
        u.setName("User " + id);
        u.setPassword("hash");
        u.setEnabled(true);
        return u;
    }

    @Test
    @DisplayName("fails when the token has no jti claim")
    void noJti_fails() {
        OAuth2TokenValidatorResult result = validator.validate(jwt(null, 1));

        assertThat(result.hasErrors()).isTrue();
        verifyNoInteractions(accessTokenRepository, userRepository);
    }

    @Test
    @DisplayName("fails when the jti is not found in the access_tokens table")
    void unknownJti_fails() {
        when(accessTokenRepository.findByJti("unknown")).thenReturn(Optional.empty());

        OAuth2TokenValidatorResult result = validator.validate(jwt("unknown", 1));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("fails when the persisted access token is marked revoked")
    void revokedToken_fails() {
        AccessTokenEntity entity = new AccessTokenEntity();
        entity.setJti("revoked-jti");
        entity.revoke();
        when(accessTokenRepository.findByJti("revoked-jti")).thenReturn(Optional.of(entity));

        OAuth2TokenValidatorResult result = validator.validate(jwt("revoked-jti", 1));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("fails when the token's user no longer exists")
    void missingUser_fails() {
        AccessTokenEntity entity = new AccessTokenEntity();
        entity.setJti("valid-jti");
        when(accessTokenRepository.findByJti("valid-jti")).thenReturn(Optional.of(entity));
        when(userRepository.findById(1)).thenReturn(Optional.empty());

        OAuth2TokenValidatorResult result = validator.validate(jwt("valid-jti", 1));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("fails when the token's user is disabled")
    void disabledUser_fails() {
        AccessTokenEntity entity = new AccessTokenEntity();
        entity.setJti("valid-jti");
        User user = enabledUser(1);
        user.setEnabled(false);
        when(accessTokenRepository.findByJti("valid-jti")).thenReturn(Optional.of(entity));
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        OAuth2TokenValidatorResult result = validator.validate(jwt("valid-jti", 1));

        assertThat(result.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("fails when the subject is not a numeric user id")
    void nonNumericSubject_fails() {
        AccessTokenEntity entity = new AccessTokenEntity();
        entity.setJti("valid-jti");
        when(accessTokenRepository.findByJti("valid-jti")).thenReturn(Optional.of(entity));

        Jwt jwt = Jwt.withTokenValue("token-value")
                .header("alg", "RS256")
                .jti("valid-jti")
                .subject("not-a-number")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("fails when the subject claim is blank")
    void blankSubject_fails() {
        AccessTokenEntity entity = new AccessTokenEntity();
        entity.setJti("valid-jti");
        when(accessTokenRepository.findByJti("valid-jti")).thenReturn(Optional.of(entity));

        Jwt jwt = Jwt.withTokenValue("token-value")
                .header("alg", "RS256")
                .jti("valid-jti")
                .subject(" ")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();

        OAuth2TokenValidatorResult result = validator.validate(jwt);

        assertThat(result.hasErrors()).isTrue();
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("succeeds for a non-revoked token belonging to an enabled user")
    void validToken_succeeds() {
        AccessTokenEntity entity = new AccessTokenEntity();
        entity.setJti("valid-jti");
        when(accessTokenRepository.findByJti("valid-jti")).thenReturn(Optional.of(entity));
        when(userRepository.findById(1)).thenReturn(Optional.of(enabledUser(1)));

        OAuth2TokenValidatorResult result = validator.validate(jwt("valid-jti", 1));

        assertThat(result.hasErrors()).isFalse();
    }
}
