package de.frauas.design.backend.auth;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

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

    @Column(name = "scopes")
    private String scopes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** True once this token has been consumed/rotated or explicitly revoked. */
    @Column(name = "revoked", nullable = false)
    private boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    /**
     * Token value of the successor token issued during rotation.
     * Allows theft-detection: if a revoked token is reused, the chain can be traced.
     */
    @Column(name = "replaced_by")
    private String replacedBy;

    /** Marks this token as consumed and records its successor. */
    public void revokeAndReplace(String successorTokenValue) {
        if (!this.revoked) {
            this.revoked = true;
            this.revokedAt = Instant.now();
            this.replacedBy = successorTokenValue;
        }
    }
}
