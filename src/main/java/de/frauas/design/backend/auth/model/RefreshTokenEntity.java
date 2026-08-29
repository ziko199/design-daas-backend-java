package de.frauas.design.backend.auth.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Persisted record for every issued opaque refresh token.
 *
 * <p>Supports rotation/revocation: a token is looked up during the refresh grant and then
 * marked revoked once it has been consumed or explicitly logged out.</p>
 */
@Entity
@Table(name = "refresh_tokens")
@Getter
@Setter
@NoArgsConstructor
public class RefreshTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_value", nullable = false, unique = true)
    private String tokenValue;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    /**
     * The single OAuth2 scope granted to this token (each user has exactly one role/scope).
     */
    @Column(name = "scope", nullable = false, length = 20)
    private String scope;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /**
     * True once this token has been consumed/rotated or explicitly revoked.
     */
    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * Marks this token as consumed/revoked (e.g. on rotation or logout).
     */
    public void revoke() {
        if (!this.revoked) {
            this.revoked = true;
            this.revokedAt = Instant.now();
        }
    }
}
