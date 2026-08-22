package de.frauas.design.backend.auth.service;

import de.frauas.design.backend.auth.exception.InvalidScopeException;
import de.frauas.design.backend.user.model.BaseUser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Resolves and validates the OAuth2 scope for a user, ensuring clients
 * cannot escalate beyond the scope their role is authorized to receive.
 * Extracted from {@link TokenService} so scope rules can be unit-tested
 * independently of grant handling.
 *
 * <p>Each user has exactly one role, so a token only ever carries a single scope
 * (no space-separated scope lists).</p>
 */
@Component
@Slf4j
public class ScopeResolver {

    /**
     * Validates the client-requested scope against the set of scopes that
     * {@code user}'s role is authorized to receive.
     *
     * <ul>
     *   <li>Regular {@code user} → only the {@code user} scope is allowed.</li>
     *   <li>{@code admin} → either {@code admin} or {@code user} is allowed.</li>
     *   <li>{@code expert} → either {@code expert} or {@code user} is allowed.</li>
     * </ul>
     *
     * <p>If the client requests no scope, the default scope for the user's role is
     * returned. If the client requests a scope outside the authorized set (or more
     * than one scope), an {@link InvalidScopeException} is thrown.</p>
     *
     * @param user           the authenticated user
     * @param requestedScope the raw scope parameter from the token request (may be null/blank)
     * @return the single authorized scope to grant
     * @throws InvalidScopeException if the requested scope exceeds the user's authorization
     */
    public String resolveAuthorizedScope(BaseUser user, String requestedScope) {
        if (requestedScope == null || requestedScope.isBlank()) {
            // Default: grant the primary scope for this role
            return user.getRole();
        }

        String requested = requestedScope.trim();
        Set<String> allowed = allowedScopesFor(user);

        // Reject multi-scope requests and anything outside the user's authorization
        if (requested.contains(" ") || !allowed.contains(requested)) {
            log.warn("resolveAuthorizedScope — scope escalation attempt by user={}: requested={}, allowed={}",
                    user.getEmail(), requested, allowed);
            throw new InvalidScopeException();
        }

        return requested;
    }

    /**
     * Returns the set of scopes a user is authorized to hold, derived solely
     * from their discriminator role — not from client input.
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
     * Resolves a scope string that was previously persisted to the database (e.g.
     * the {@code scope} column of a refresh token). Because this value was already
     * validated at issuance time, no role-check is needed here.
     */
    public String parsePersistedScope(String scope) {
        return (scope == null || scope.isBlank()) ? "user" : scope.trim();
    }
}
