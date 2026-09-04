package de.frauas.design.backend.config;

import de.frauas.design.backend.config.properties.CorsProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Configures cross-origin resource sharing for the API.
 *
 * <p>Origins are explicitly configured through {@link CorsProperties}
 * instead of using a wildcard. This prevents unintended cross-origin
 * access to the API.</p>
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private static final List<String> ALLOWED_METHODS = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");

    private final CorsProperties corsProperties;

    /**
     * Creates the CORS configuration used by Spring Security.
     *
     * @return configured CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(corsProperties.getAllowedOrigins());
        config.setAllowedMethods(ALLOWED_METHODS);
        config.setAllowedHeaders(List.of("*"));

        /* Keep this enabled only if the frontend requires browser-managed
         * credentials such as cookies. Bearer tokens sent through the
         * Authorization header normally do not require this setting.
         */
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
