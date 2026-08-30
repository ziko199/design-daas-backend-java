package de.frauas.design.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration properties for cross-origin request handling.
 *
 * <p>Origins must be explicitly listed rather than using a wildcard.
 * This is especially important when credentialed cross-origin requests
 * are enabled.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.cors")
public class CorsProperties {

    /**
     * Exact origins allowed to access the API.
     *
     * <p>Each origin must contain scheme, host, and optional port,
     * for example {@code http://localhost:3000}.</p>
     */
    private List<String> allowedOrigins = List.of("http://localhost:3000");
}
