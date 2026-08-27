package de.frauas.design.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configuration properties for cross-origin request handling.
 *
 * <p>{@code allowedOrigins} must be an explicit allow-list — never a wildcard — because
 * {@link CorsConfig} enables {@code allowCredentials}. Combining a wildcard origin with
 * credentials would let any origin make authenticated cross-site requests.</p>
 */
@Component
@ConfigurationProperties(prefix = "app.cors")
@Getter
@Setter
public class CorsProperties {

    /** Exact origins (scheme + host + port) allowed to make credentialed cross-origin requests. */
    private List<String> allowedOrigins = List.of("http://localhost:3000");
}
