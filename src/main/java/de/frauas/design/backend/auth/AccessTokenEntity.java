package de.frauas.design.backend.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Persisted record for every issued JWT access token.
 * Enables revocation: OAuth2SessionController looks up the jti claim
 * and rejects the request if revoked=true.
 *
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

    /** JWT ID (jti) claim – the primary lookup key. */
    @Column(name = "jti", nullable = false, unique = true, length = 200)
    private String jti;

    @Column(name = "user_id", nullable = false)
    private Integer userId;

    @Column(name = "scopes", nullable = false, length = 500)
    private String scopes;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /** Marks this token as revoked (e.g. on logout). */
    public void revoke() {
        if (!this.revoked) {
            this.revoked = true;
            this.revokedAt = Instant.now();
        }
    }
}

