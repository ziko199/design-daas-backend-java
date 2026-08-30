package de.frauas.design.backend.shared.security;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Thread-safe token bucket implementing a continuous refill rate.
 *
 * <p>The bucket starts full and refills continuously at a rate equivalent
 * to {@code capacity} tokens per refill period. A successful request consumes
 * one token.</p>
 *
 * <p>The implementation stores the current token count and last refill time
 * using atomic state and does not require explicit locking.</p>
 */
final class TokenBucket {

    private final long capacity;
    private final double refillTokensPerMillisecond;

    /**
     * Packed state: [epochMinute (32 bits) | consumedThisMinute (32 bits)]
     */
    private final AtomicLong state;

    TokenBucket(int requestsPerMinute, long refillPeriodMillis) {
        if (requestsPerMinute <= 0) {
            throw new IllegalArgumentException(
                    "requestsPerMinute must be greater than zero"
            );
        }

        if (refillPeriodMillis <= 0) {
            throw new IllegalArgumentException(
                    "refillPeriodMillis must be greater than zero"
            );
        }

        this.capacity = requestsPerMinute;
        this.refillTokensPerMillisecond = (double) requestsPerMinute / refillPeriodMillis;

        /*
         * State stores:
         * high 32 bits = last refill timestamp (seconds)
         * low 32 bits  = token count
         *
         * This implementation is intentionally simple for the relatively
         * small request limits used by the application.
         */
        long timestampSeconds = System.currentTimeMillis() / 1_000L;

        this.state = new AtomicLong((timestampSeconds << 32) | requestsPerMinute);
    }

    boolean tryConsume() {
        while (true) {
            long current = state.get();

            long lastRefillSeconds = current >>> 32;
            long storedTokens = current & 0xFFFFFFFFL;

            long nowMillis = System.currentTimeMillis();
            long lastRefillMillis = lastRefillSeconds * 1_000L;

            long elapsedMillis = Math.max(0, nowMillis - lastRefillMillis);

            long refilledTokens = (long) (elapsedMillis * refillTokensPerMillisecond);

            long availableTokens = Math.min(capacity, storedTokens + refilledTokens);

            if (availableTokens <= 0) {
                return false;
            }

            long nowSeconds = nowMillis / 1_000L;

            long nextState = (nowSeconds << 32) | (availableTokens - 1);

            if (state.compareAndSet(current, nextState)) {
                return true;
            }
        }
    }
}


