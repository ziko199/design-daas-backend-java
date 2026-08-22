package de.frauas.design.backend.auth.service;

import de.frauas.design.backend.auth.exception.InvalidScopeException;
import de.frauas.design.backend.user.model.BaseUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * SEC-H5: Resolves and validates OAuth2 scopes for a user, ensuring clients
 * cannot escalate beyond the scopes their role is authorized to receive.
 * Extracted from {@link TokenService} so scope rules can be unit-tested
 * independently of grant handling.
 */
@Component
@Slf4j
public class ScopeResolver {

    /**
     * Validates the client-requested scope string against the set of scopes
     * that {@code user}'s role is authorized to receive.
     *
     * <ul>
     *   <li>Regular {@code user} → only the {@code user} scope is allowed.</li>
     *   <li>{@code admin} → both {@code admin} and {@code user} scopes are allowed.</li>
     *   <li>{@code expert} → both {@code expert} and {@code user} scopes are allowed.</li>
     * </ul>
     *
     * <p>If the client requests no scope, the default scope for the user's role is
     * returned. If the client requests a scope outside the authorized set, an
     * {@link InvalidScopeException} is thrown.</p>
     *
     * @param user           the authenticated user
     * @param requestedScope the raw scope parameter from the token request (may be null/blank)
     * @return the authorized scope set to grant
     * @throws InvalidScopeException if any requested scope exceeds the user's authorization
     */
    public Set<String> resolveAuthorizedScopes(BaseUser user, String requestedScope) {
        Set<String> allowed = allowedScopesFor(user);

        if (requestedScope == null || requestedScope.isBlank()) {
            // Default: grant only the primary scope for this role
            return Set.of(user.getRole());
        }

        Set<String> requested = new HashSet<>(Arrays.asList(requestedScope.trim().split("\\s+")));
        requested.removeIf(String::isBlank);

        // Reject the entire request if any requested scope exceeds the user's authorization
        if (!allowed.containsAll(requested)) {
            log.warn("resolveAuthorizedScopes — scope escalation attempt by user={}: requested={}, allowed={}",
                    user.getEmail(), requested, allowed);
            throw new InvalidScopeException();
        }

        return requested;
    }

    /**
     * Returns the complete set of scopes a user is authorized to hold, derived
     * solely from their discriminator role — not from client input.
     */
    private Set<String> allowedScopesFor(BaseUser user) {
        return switch (user.getRole()) {
            case "admin" -> Set.of("admin", "user");
            case "expert" -> Set.of("expert", "user");
            case "user" -> Set.of("user");
            default -> Set.of("user");
        };
    }

    /**
     * Parses a space-separated scope string that was previously persisted to the
     * database (e.g. the {@code scopes} column of a refresh token). Because these
     * values were already validated at issuance time no role-check is needed here.
     */
    public Set<String> parsePersistedScopes(String scope) {
        if (scope == null || scope.isBlank()) {
            return Set.of("user");
        }
        // use \\s+ (one-or-more whitespace) — not the character class [\\s+]
        return new HashSet<>(Arrays.asList(scope.trim().split("\\s+")));
    }
}
