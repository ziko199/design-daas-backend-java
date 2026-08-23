package de.frauas.design.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

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
 * naming. Each user has exactly one role, so {@code scope} is always a single value
 * (never a space-separated list).</p>
 *
 * @param accessToken       the signed JWT access token
 * @param tokenType         the token type, always {@code "Bearer"} per RFC 6749 §5.1
 * @param refreshToken      the opaque refresh token to redeem for a new pair later
 * @param scope             the single scope actually granted
 * @param expiresInSeconds  remaining lifetime of {@code accessToken}, in seconds
 */
public record TokenResponseDto(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("token_type") String tokenType,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("scope") String scope,
        @JsonProperty("expires_in") long expiresInSeconds) {}
