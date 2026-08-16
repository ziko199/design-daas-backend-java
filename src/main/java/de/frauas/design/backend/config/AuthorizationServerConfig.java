package de.frauas.design.backend.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
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
import java.util.Set;
import java.util.UUID;

/**
 * Provides the RSA JWK source, JwtEncoder and JwtDecoder beans used by
 * {@link de.frauas.design.backend.auth.TokenController} (issuance) and
 * {@link de.frauas.design.backend.config.SecurityConfig} (resource-server validation).
 *
 * <p>Spring Boot 4.x / Spring Security 7.x removed
 * {@code OAuth2AuthorizationServerConfiguration.jwtDecoder()}.
 * The equivalent is now {@code NimbusJwtDecoder.withJwkSource(jwkSource).build()}
 * from the {@code spring-security-oauth2-jose} module (bundled with
 * {@code spring-boot-starter-oauth2-resource-server}).</p>
 */
@Configuration
public class AuthorizationServerConfig {

    @Value("${app.oauth2.keys-dir:./keys}")
    private String keysDir;

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
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return NimbusJwtDecoder.withJwkSource(jwkSource).build();
    }

    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    // -------------------------------------------------------------------------
    // RSA key persistence
    // -------------------------------------------------------------------------

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

    /** Sets file permissions to owner-read/write only (600). No-op on non-POSIX systems. */
    private static void restrictFilePermissions(Path path) {
        try {
            Files.setPosixFilePermissions(path, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE
            ));
        } catch (UnsupportedOperationException ignored) {
            // Non-POSIX filesystem (e.g. Windows dev environment) — skip silently
        } catch (Exception e) {
            // Log but don't fail startup — use SLF4J, not System.err
            org.slf4j.LoggerFactory.getLogger(AuthorizationServerConfig.class)
                .warn("Could not set RSA key file permissions: {}", e.getMessage());
        }
    }
}
