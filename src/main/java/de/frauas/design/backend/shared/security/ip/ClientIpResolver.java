package de.frauas.design.backend.shared.security.ip;

import de.frauas.design.backend.config.properties.RateLimitingProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.net.InetAddress;

/**
 * Resolves the client IP address for rate-limiting purposes.
 *
 * <p>{@code X-Forwarded-For} is only trusted when the direct connection
 * originates from a configured trusted reverse proxy.</p>
 */
@Component
@RequiredArgsConstructor
public class ClientIpResolver {

    private static final String FORWARDED_FOR_HEADER = "X-Forwarded-For";

    private final RateLimitingProperties properties;

    /**
     * Resolves the originating client IP address.
     *
     * @param request current HTTP request
     * @return resolved client IP address
     */
    public String resolve(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        String forwardedFor = request.getHeader(FORWARDED_FOR_HEADER);

        if (forwardedFor != null && !forwardedFor.isBlank() && isTrustedProxy(remoteAddress)) {

            return forwardedFor.split(",")[0].trim();
        }

        return remoteAddress;
    }

    private boolean isTrustedProxy(String remoteAddress) {
        return properties.getTrustedProxyRanges().stream().anyMatch(cidr -> matchesCidr(remoteAddress, cidr));
    }

    private boolean matchesCidr(String address, String cidr) {
        try {
            String[] parts = cidr.split("/", 2);

            InetAddress network = InetAddress.getByName(parts[0]);
            InetAddress candidate = InetAddress.getByName(address);

            if (network.getAddress().length != candidate.getAddress().length) {
                return false;
            }

            int prefixLength = parts.length == 2 ? Integer.parseInt(parts[1]) : network.getAddress().length * 8;

            if (prefixLength < 0 || prefixLength > network.getAddress().length * 8) {
                return false;
            }

            byte[] networkBytes = network.getAddress();
            byte[] candidateBytes = candidate.getAddress();

            int fullBytes = prefixLength / 8;
            int remainingBits = prefixLength % 8;

            for (int i = 0; i < fullBytes; i++) {
                if (networkBytes[i] != candidateBytes[i]) {
                    return false;
                }
            }

            if (remainingBits == 0) {
                return true;
            }

            int mask = 0xFF << (8 - remainingBits);

            return (networkBytes[fullBytes] & mask) == (candidateBytes[fullBytes] & mask);

        } catch (Exception ignored) {
            return false;
        }
    }
}
