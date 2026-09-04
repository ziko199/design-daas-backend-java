package de.frauas.design.backend.auth.dto;

import de.frauas.design.backend.auth.exception.UnsupportedGrantTypeException;

/**
 * A validated, typed representation of an incoming {@code POST /oauth2/user/token}
 * request, replacing a flat {@code (grantType, username, password, refreshToken, scope)}
 * parameter list with one purpose-built type per grant.
 *
 * <p>Only fields relevant to the actual grant are present on each variant — e.g.
 * {@link RefreshGrantRequest} has no {@code username}/{@code password}/{@code scope} to
 * ignore. {@link #of} is the single place that maps the raw {@code grant_type} string
 * onto the right variant (or rejects it), so that string literal only appears once.</p>
 *
 * <p>Being a {@code sealed} interface, {@code TokenService} can dispatch on it with an
 * exhaustive {@code switch} — the compiler guarantees every variant is handled, so
 * adding a new grant type without updating the dispatcher is a compile error rather
 * than a silently-ignored case.</p>
 */
public sealed interface GrantRequest {

    /** {@code grant_type} value for the resource-owner password grant (RFC 6749 §4.3). */
    String GRANT_TYPE_PASSWORD = "password";

    /** {@code grant_type} value for the refresh-token grant (RFC 6749 §6). */
    String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";

    /**
     * Maps a raw {@code grant_type} value and its associated request parameters onto
     * the matching {@link GrantRequest} variant.
     *
     * @throws UnsupportedGrantTypeException if {@code grantType} is neither
     *                                       {@value #GRANT_TYPE_PASSWORD} nor {@value #GRANT_TYPE_REFRESH_TOKEN}
     */
    static GrantRequest of(String grantType, String username, String password, String refreshToken, String scope) {
        if (grantType == null || grantType.isBlank()) {
            throw new UnsupportedGrantTypeException();
        }
        return switch (grantType) {
            case GRANT_TYPE_PASSWORD -> new PasswordGrantRequest(username, password, scope);
            case GRANT_TYPE_REFRESH_TOKEN -> new RefreshGrantRequest(refreshToken);
            default -> throw new UnsupportedGrantTypeException();
        };
    }

    /**
     * Resource-owner password grant request: credentials plus the requested scope.
     */
    record PasswordGrantRequest(String username, String password, String scope) implements GrantRequest {}

    /**
     * Refresh-token grant request: the opaque refresh token to redeem.
     */
    record RefreshGrantRequest(String refreshToken) implements GrantRequest {}
}
