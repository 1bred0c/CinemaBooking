package congtuong.dev.cinemabooking.security.jwt;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleanupScheduler {
    private final RefreshTokenService refreshTokenService;

    @Scheduled(fixedDelayString = "${security.jwt.refresh-token-cleanup-interval:1h}")
    public void cleanup() {
        int deleted = refreshTokenService.deleteExpiredAndRevokedTokens();
        if (deleted > 0) {
            log.info("Deleted {} expired or revoked refresh tokens", deleted);
        }
    }
}
