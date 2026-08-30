package de.frauas.design.backend.shared.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Coordinates rate limiting for clients and endpoints.
 *
 * <p>Each client/IP and endpoint combination has its own token bucket.</p>
 */
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private static final long REFILL_PERIOD_MILLIS = 60_000L;

    /**
     * Cache of token-bucket state keyed by "{clientIp}|{servletPath}".
     */
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    /**
     * Attempts to consume one token for the given client and endpoint.
     *
     * @param clientId client identifier, normally the resolved IP address
     * @param endpoint protected endpoint
     * @param requestsPerMinute maximum refill rate
     * @return {@code true} if the request is allowed
     */
    public boolean tryConsume(String clientId, String endpoint, int requestsPerMinute) {

        String key = clientId + "|" + endpoint;

        TokenBucket bucket =
                buckets.computeIfAbsent(key, ignored -> new TokenBucket(requestsPerMinute, REFILL_PERIOD_MILLIS));

        return bucket.tryConsume();
    }
}
