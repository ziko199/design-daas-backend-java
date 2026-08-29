package de.frauas.design.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Configuration properties for rate-limiting / brute-force protection.
 *
 * <p>All limits are per-IP-address and use a sliding (greedy-refill) token bucket.
 * Set {@code app.rate-limiting.enabled=false} in tests to bypass the filter.</p>
 */
@Component
@ConfigurationProperties(prefix = "app.rate-limiting")
@Getter
@Setter
public class RateLimitingProperties {

    /** Master switch -- set to false in test profiles to bypass the filter. */
    private boolean enabled = true;

    /** Max requests per minute per IP for POST /oauth2/user/token. */
    private int authTokenRequestsPerMinute = 20;

    /** Max requests per minute per IP for POST /user (registration). */
    private int registerRequestsPerMinute = 5;

    /** Max requests per minute per IP for POST /user/validate_email. */
    private int validateEmailRequestsPerMinute = 10;

    /**
     * Trusted reverse-proxy CIDR ranges. Only requests arriving from one of these
     * ranges are allowed to use the {@code X-Forwarded-For} header for IP resolution.
     * All other requests use {@code remoteAddr} directly to prevent spoofing.
     */
    private List<String> trustedProxyRanges = List.of("127.0.0.1/32", "::1/128");
}
