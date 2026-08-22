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
 * Persisted record for every issued JWT access token.
 * Enables revocation: {@code TokenRevocationValidator} looks up the jti claim on every
 * authenticated request and rejects it if {@code revoked=true} (or the record is missing).
 * <p>
 * Mirrors the PHP oauth2_access_token table.
 */
@Entity
@Table(name = "access_tokens")
@Getter
@Setter
@NoArgsConstructor
public class AccessTokenEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * JWT ID (jti) claim – the primary lookup key.
     */
    @Column(name = "jti", nullable = false, unique = true, length = 200)
    private String jti;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    /**
     * The single OAuth2 scope granted to this token (each user has exactly one role/scope).
     */
    @Column(name = "scope", nullable = false, length = 20)
    private String scope;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * Marks this token as revoked (e.g. on logout).
     */
    public void revoke() {
        if (!this.revoked) {
            this.revoked = true;
            this.revokedAt = Instant.now();
        }
    }
}
