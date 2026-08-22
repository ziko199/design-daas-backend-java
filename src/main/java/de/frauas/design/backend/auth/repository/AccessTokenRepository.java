package de.frauas.design.backend.auth.repository;

import de.frauas.design.backend.auth.model.AccessTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface AccessTokenRepository extends JpaRepository<AccessTokenEntity, Long> {

    /**
     * Find by JWT ID claim — used for revocation checks in TokenRevocationValidator / SessionService.
     */
    Optional<AccessTokenEntity> findByJti(String jti);

    @Modifying
    @Transactional
    @Query("DELETE FROM AccessTokenEntity at WHERE at.userId = :userId")
    void deleteByUserId(@Param("userId") Integer userId);

    @Modifying
    @Transactional
    @Query("DELETE FROM AccessTokenEntity at WHERE at.expiresAt < :now")
    void deleteExpired(@Param("now") Instant now);
}

