package de.frauas.design.backend.auth.service;

import de.frauas.design.backend.auth.repository.AccessTokenRepository;
import de.frauas.design.backend.auth.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * SEC-M7: Scheduled job that purges expired tokens from both the
 * {@code refresh_tokens} and {@code access_tokens} tables.
 *
 * <p>Without this job the tables grow without bound because expired tokens
 * are never removed — {@code RefreshTokenRepository.deleteExpired()} and
 * {@code AccessTokenRepository.deleteExpired()} were defined but never called.</p>
 *
 * <p>The job runs daily at 03:00 (server local time). The schedule is
 * configurable via the {@code app.token.cleanup.cron} property.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessTokenRepository accessTokenRepository;

    /**
     * Deletes all refresh tokens and access tokens whose {@code expiresAt}
     * timestamp is strictly before {@code now}.
     *
     * <p>Default schedule: daily at 03:00 (cron {@code "0 0 3 * * *"}).
     * Override with {@code app.token.cleanup.cron} in {@code application.yml}.</p>
     */
    @Scheduled(cron = "${app.token.cleanup.cron:0 0 3 * * *}")
    @Transactional
    public void purgeExpiredTokens() {
        Instant now = Instant.now();
        refreshTokenRepository.deleteExpired(now);
        accessTokenRepository.deleteExpired(now);
        log.info("Token cleanup completed — purged all tokens expired before {}", now);
    }
}

