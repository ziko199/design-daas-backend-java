package de.frauas.design.backend.shared.security;

/**
 * Central place for the OAuth2 scope/authority string constants used across
 * {@code @PreAuthorize} expressions and security configuration.
 *
 * <p>Avoids the {@code "SCOPE_admin"} magic string being duplicated (and potentially
 * mistyped) across every controller that needs it.</p>
 */
public final class Authorities {

    public static final String SCOPE_ADMIN = "SCOPE_admin";
    public static final String SCOPE_USER = "SCOPE_user";

    /**
     * Ready-to-use {@code @PreAuthorize} expression requiring the admin scope.
     */
    public static final String IS_ADMIN = "hasAuthority('" + SCOPE_ADMIN + "')";

    /**
     * Ready-to-use {@code @PreAuthorize} expression allowing any authenticated scope.
     */
    public static final String IS_ANY_USER = "hasAnyAuthority('" + SCOPE_USER + "', '" + SCOPE_ADMIN + "')";

    private Authorities() {}
}
