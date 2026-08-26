package de.frauas.design.backend.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import de.frauas.design.backend.auth.repository.AccessTokenRepository;
import de.frauas.design.backend.auth.service.TokenRevocationValidator;
import de.frauas.design.backend.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Provides the RSA JWK source, JwtEncoder and JwtDecoder beans used by
 * {@link de.frauas.design.backend.auth.service.TokenService} (issuance) and
 * {@link de.frauas.design.backend.config.SecurityConfig} (resource-server validation).
 *
 * <p>Spring Boot 4.x / Spring Security 7.x removed
 * {@code OAuth2AuthorizationServerConfiguration.jwtDecoder()}.
 * The equivalent is now {@code NimbusJwtDecoder.withJwkSource(jwkSource).build()}
 * from the {@code spring-security-oauth2-jose} module (bundled with
 * {@code spring-boot-starter-oauth2-resource-server}).</p>
 *
 * <p>The decoder's validator chain combines Spring's default checks (expiry, not-before)
 * with an issuer check (matching {@code app.oauth2.issuer}, the value {@code TokenIssuer}
 * signs into every access token) and {@link TokenRevocationValidator}, so that every
 * authenticated request — not just {@code GET /oauth2/user/session} — rejects revoked
 * access tokens and tokens belonging to disabled/deleted users.</p>
 */
@Configuration
@Slf4j
public class AuthorizationServerConfig {

    @Value("${app.oauth2.keys-dir:./keys}")
    private String keysDir;

    @Value("${app.oauth2.issuer:http://localhost:8080}")
    private String issuer;

    /** Sets file permissions to owner-read/write only (600). No-op on non-POSIX systems. */
    private static void restrictFilePermissions(Path path) {
        try {
            Files.setPosixFilePermissions(
                    path, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException ignored) {
            // Non-POSIX filesystem (e.g. Windows dev environment) — skip silently
        } catch (Exception e) {
            // Log but don't fail startup
            log.warn("Could not set RSA key file permissions: {}", e.getMessage());
        }
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() throws Exception {
        RSAKey rsaKey = loadOrGenerateRsaKey();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    /**
     * Spring Security 7.x: use {@code NimbusJwtDecoder.withJwkSource()} instead of
     * the removed {@code OAuth2AuthorizationServerConfiguration.jwtDecoder()}.
     */
    @Bean
    public JwtDecoder jwtDecoder(
            JWKSource<SecurityContext> jwkSource,
            AccessTokenRepository accessTokenRepository,
            UserRepository userRepository) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSource(jwkSource).build();
        OAuth2TokenValidator<Jwt> revocationValidator =
                new TokenRevocationValidator(accessTokenRepository, userRepository);
        OAuth2TokenValidator<Jwt> issuerValidator = new JwtClaimValidator<>("iss", issuer::equals);
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                List.of(JwtValidators.createDefault(), issuerValidator, revocationValidator)));
        return decoder;
    }

    // -------------------------------------------------------------------------
    // RSA key persistence
    // -------------------------------------------------------------------------

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    private RSAKey loadOrGenerateRsaKey() throws Exception {
        Path keysPath = Paths.get(keysDir);
        Files.createDirectories(keysPath);
        Path jwkPath = keysPath.resolve("rsa-jwk.json");

        if (Files.exists(jwkPath)) {
            return RSAKey.parse(Files.readString(jwkPath));
        }

        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair kp = gen.generateKeyPair();

        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) kp.getPublic())
                .privateKey((RSAPrivateKey) kp.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();

        Files.writeString(jwkPath, rsaKey.toJSONString());
        restrictFilePermissions(jwkPath);
        return rsaKey;
    }
}
