package de.frauas.design.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * SEC-H3 — IP-based rate-limiting filter for unauthenticated auth endpoints.
 *
 * <p>Uses a greedy token-bucket (implemented with lock-free {@link AtomicLong}s)
 * to enforce per-minute request limits per client IP.  Excess requests are
 * rejected with HTTP 429 before any controller or Spring Security logic runs.</p>
 *
 * <p>The filter is ordered at -200 (before the Spring Security filter chain at
 * -100) so rate-limited requests are dropped at the earliest possible stage.</p>
 *
 * <p>Disable via {@code app.rate-limiting.enabled=false} (e.g. in test profiles).</p>
 */
@Component
@Order(-200)
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingProperties props;

    /** Cache of token-bucket state keyed by "{clientIp}|{servletPath}". */
    private final ConcurrentHashMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    /** Checks whether {@code ip} falls within the given {@code cidr} (IPv4 only). */
    private static boolean isInCidr(String ip, String cidr) throws Exception {
        String[] parts = cidr.split("/");
        if (parts.length != 2) return ip.equals(cidr);
        int prefix = Integer.parseInt(parts[1]);
        byte[] network = InetAddress.getByName(parts[0]).getAddress();
        byte[] addr = InetAddress.getByName(ip).getAddress();
        if (network.length != addr.length) return false; // IPv4 vs IPv6 mismatch
        int fullBytes = prefix / 8;
        int remainder = prefix % 8;
        for (int i = 0; i < fullBytes; i++) {
            if (network[i] != addr[i]) return false;
        }
        if (remainder > 0 && fullBytes < network.length) {
            int mask = 0xFF & (0xFF << (8 - remainder));
            return (network[fullBytes] & mask) == (addr[fullBytes] & mask);
        }
        return true;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!props.isEnabled()) {
            return true;
        }
        return !(HttpMethod.POST.matches(request.getMethod()) && isRateLimitedPath(request.getServletPath()));
    }

    private boolean isRateLimitedPath(String path) {
        return "/oauth2/user/token".equals(path) || "/user".equals(path) || "/user/validate_email".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String ip = resolveClientIp(request);
        String path = request.getServletPath();
        String bucketKey = ip + "|" + path;

        TokenBucket bucket = buckets.computeIfAbsent(bucketKey, k -> new TokenBucket(capacityFor(path)));

        if (bucket.tryConsume()) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded: ip={} path={}", ip, path);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter()
                    .write("{\"error\":\"too_many_requests\","
                            + "\"error_description\":\"Too many requests — please try again later\"}");
        }
    }

    private int capacityFor(String path) {
        return switch (path) {
            case "/oauth2/user/token" -> props.getAuthTokenRequestsPerMinute();
            case "/user" -> props.getRegisterRequestsPerMinute();
            case "/user/validate_email" -> props.getValidateEmailRequestsPerMinute();
            default -> 60;
        };
    }

    /**
     * Evicts token buckets that have not been touched for at least 2 minutes.
     *
     * <p>Without this, {@link #buckets} would grow without bound over the process
     * lifetime as new client IPs make requests, since entries are never otherwise
     * removed.</p>
     */
    @Scheduled(fixedRate = 5, timeUnit = TimeUnit.MINUTES)
    void evictStaleBuckets() {
        long currentEpochMinute = System.currentTimeMillis() / 60_000L;
        int sizeBefore = buckets.size();
        buckets.values().removeIf(bucket -> bucket.isStale(currentEpochMinute));
        int evicted = sizeBefore - buckets.size();
        if (evicted > 0) {
            log.debug("Rate-limit bucket cleanup: evicted {} stale entries, {} remaining", evicted, buckets.size());
        }
    }

    /**
     * Resolves the real client IP. {@code X-Forwarded-For} is only trusted when
     * the direct connection ({@code remoteAddr}) comes from a configured trusted
     * proxy CIDR — otherwise the header is ignored to prevent spoofing.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank() && isFromTrustedProxy(remoteAddr)) {
            return forwarded.split(",")[0].trim();
        }
        return remoteAddr;
    }

    private boolean isFromTrustedProxy(String remoteAddr) {
        for (String cidr : props.getTrustedProxyRanges()) {
            try {
                if (isInCidr(remoteAddr, cidr)) return true;
            } catch (Exception ignored) {
                // Skip malformed CIDR entries
            }
        }
        return false;
    }

    // -------------------------------------------------------------------------
    // Inner class: lock-free greedy token bucket
    // -------------------------------------------------------------------------

    /**
     * Greedy token-bucket with a one-minute refill window.
     *
     * <p>State is encoded in a single {@code long}: the high 32 bits hold the
     * epoch-minute when the window started; the low 32 bits hold the number of
     * tokens already consumed in that window.  CAS ensures thread safety without
     * locking.</p>
     */
    static final class TokenBucket {

        private final int capacity;
        /** Packed state: [epochMinute (32 bits) | consumedThisMinute (32 bits)] */
        private final AtomicLong state;

        TokenBucket(int requestsPerMinute) {
            this.capacity = requestsPerMinute;
            long epochMinute = System.currentTimeMillis() / 60_000L;
            this.state = new AtomicLong(epochMinute << 32);
        }

        boolean tryConsume() {
            while (true) {
                long current = state.get();
                long epochMinute = System.currentTimeMillis() / 60_000L;
                long storedMin = current >>> 32;
                long consumed = current & 0xFFFFFFFFL;

                if (storedMin != epochMinute) {
                    // New minute: reset counter to 1 (for this request)
                    long next = (epochMinute << 32) | 1L;
                    if (state.compareAndSet(current, next)) {
                        return true;
                    }
                    // CAS failed — another thread updated first; retry
                    continue;
                }

                if (consumed >= capacity) {
                    return false; // bucket exhausted
                }

                long next = (storedMin << 32) | (consumed + 1);
                if (state.compareAndSet(current, next)) {
                    return true;
                }
                // CAS failed — retry
            }
        }

        /** True if this bucket's window is at least 2 minutes older than {@code currentEpochMinute}. */
        boolean isStale(long currentEpochMinute) {
            long storedMin = state.get() >>> 32;
            return currentEpochMinute - storedMin >= 2;
        }
    }
}
