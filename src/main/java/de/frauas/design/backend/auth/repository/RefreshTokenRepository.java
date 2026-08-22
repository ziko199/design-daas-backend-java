package de.frauas.design.backend.auth.repository;

import de.frauas.design.backend.auth.model.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, Long> {

    /**
     * Returns the token only if it has not been revoked — the primary grant lookup.
     */
    @Query("SELECT rt FROM RefreshTokenEntity rt WHERE rt.tokenValue = :tokenValue AND rt.revoked = false")
    Optional<RefreshTokenEntity> findActiveByTokenValue(@Param("tokenValue") String tokenValue);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.userId = :userId")
    void deleteByUserId(@Param("userId") Integer userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM RefreshTokenEntity rt WHERE rt.expiresAt < :now")
    void deleteExpired(@Param("now") Instant now);
}
