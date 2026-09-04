package de.frauas.design.backend.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import de.frauas.design.backend.auth.repository.AccessTokenRepository;
import de.frauas.design.backend.auth.service.TokenRevocationValidator;
import de.frauas.design.backend.config.properties.OAuth2Properties;
import de.frauas.design.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Configures RSA keys and JWT encoding/decoding for the application's OAuth2 infrastructure.
 *
 * <p>The RSA key pair is persisted locally so that issued tokens remain verifiable
 * after application restarts.</p>
 *
 * <p>Incoming JWTs are validated for standard claims, issuer, and application-level
 * token revocation.</p>
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class AuthorizationServerConfig {

    private static final String RSA_KEY_FILE = "rsa-jwk.json";
    private static final int RSA_KEY_SIZE = 2048;

    private final OAuth2Properties oauth2Properties;

    /**
     * Creates the JWK source used for signing and validating JWTs.
     *
     * @return RSA JWK source
     * @throws Exception if the key cannot be loaded or generated
     */
    @Bean
    public JWKSource<SecurityContext> jwkSource() throws Exception {
        RSAKey rsaKey = loadOrGenerateRsaKey();
        return new ImmutableJWKSet<>(new JWKSet(rsaKey));
    }

    /**
     * Creates the JWT encoder used for issuing access tokens.
     *
     * @param jwkSource configured RSA JWK source
     * @return JWT encoder
     */
    @Bean
    public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
        return new NimbusJwtEncoder(jwkSource);
    }

    /**
     * Creates the JWT decoder used by Spring Security to authenticate requests.
     *
     * <p>The validator chain performs standard JWT validation, issuer validation,
     * and application-level token revocation checks.</p>
     *
     * @param jwkSource RSA JWK source
     * @param accessTokenRepository repository containing issued/revoked tokens
     * @param userRepository repository used to verify user state
     * @return configured JWT decoder
     */
    @Bean
    public JwtDecoder jwtDecoder(
            JWKSource<SecurityContext> jwkSource,
            AccessTokenRepository accessTokenRepository,
            UserRepository userRepository) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSource(jwkSource).build();

        OAuth2TokenValidator<Jwt> issuerValidator =
                new JwtClaimValidator<>("iss", oauth2Properties.getIssuer()::equals);

        OAuth2TokenValidator<Jwt> revocationValidator =
                new TokenRevocationValidator(accessTokenRepository, userRepository);

        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                List.of(JwtValidators.createDefault(), issuerValidator, revocationValidator)));

        return decoder;
    }

    private RSAKey loadOrGenerateRsaKey() throws Exception {
        Path keyDirectory = Path.of(oauth2Properties.getKeysDir());
        Files.createDirectories(keyDirectory);

        Path keyFile = keyDirectory.resolve(RSA_KEY_FILE);

        if (Files.exists(keyFile)) {
            return RSAKey.parse(Files.readString(keyFile));
        }

        RSAKey rsaKey = generateRsaKey();

        Files.writeString(keyFile, rsaKey.toJSONString());
        restrictFilePermissions(keyFile);

        log.info("Generated new RSA signing key at {}", keyFile.toAbsolutePath());

        return rsaKey;
    }

    private RSAKey generateRsaKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(RSA_KEY_SIZE);

        KeyPair keyPair = generator.generateKeyPair();

        return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .privateKey((RSAPrivateKey) keyPair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    /**
     * Restricts the persisted private-key file to owner read/write permissions
     * on POSIX-compatible filesystems.
     *
     * <p>Windows and other non-POSIX filesystems do not support this permission
     * model and are therefore ignored.</p>
     */
    private void restrictFilePermissions(Path path) {
        try {
            Files.setPosixFilePermissions(
                    path, Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE));
        } catch (UnsupportedOperationException ignored) {
            // Filesystem does not support POSIX permissions.
        } catch (Exception exception) {
            log.warn("Unable to restrict permissions for RSA key file: {}", path.toAbsolutePath(), exception);
        }
    }
}
