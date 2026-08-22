package de.frauas.design.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * A successfully issued access/refresh token pair, returned by {@code TokenService}
 * once a password or refresh_token grant has been fully validated, and serialized
 * directly as the JSON body of {@code POST /oauth2/user/token}.
 *
 * <p>Rejected grant attempts (bad credentials, locked/disabled account, scope
 * escalation, invalid/expired refresh token, ...) are no longer represented here —
 * {@code TokenService} throws a {@code TokenGrantException} subclass instead, which
 * {@code TokenExceptionHandler} maps to the appropriate HTTP response.</p>
 *
 * <p>JSON field names follow the OAuth2 token-response convention (snake_case)
 * expected by the frontend and other API consumers, regardless of Java's camelCase
 * naming. {@code scopes} is kept internally as a {@link Set} for easy handling in
 * {@code TokenService}, but is exposed to clients as the single space-separated
 * {@code scope} field required by RFC 6749.</p>
 *
 * @param accessToken       the signed JWT access token
 * @param tokenType         the token type, always {@code "Bearer"} per RFC 6749 §5.1
 * @param refreshToken      the opaque refresh token to redeem for a new pair later
 * @param scopes            the scopes actually granted (may be a subset of what was requested)
 * @param expiresInSeconds  remaining lifetime of {@code accessToken}, in seconds
 */
public record TokenResponseDto(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonIgnore Set<String> scopes,
        @JsonProperty("expires_in") long expiresInSeconds) {

    /**
     * The granted scopes as a single space-separated string, per RFC 6749 §3.3.
     * This is what's actually serialized as the {@code scope} JSON field.
     */
    @JsonProperty("scope")
    public String scope() {
        return String.join(" ", scopes);
    }
}
