package de.frauas.design.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provides password hashing configuration.
 *
 * <p>The password encoder is defined separately so authentication-related
 * components can depend on the {@link PasswordEncoder} abstraction without
 * creating unnecessary dependencies between security configuration classes.</p>
 */
@Configuration
public class PasswordConfig {

    private static final int BCRYPT_STRENGTH = 12;

    /**
     * Creates the application's password encoder.
     *
     * @return BCrypt password encoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(BCRYPT_STRENGTH);
    }
}
