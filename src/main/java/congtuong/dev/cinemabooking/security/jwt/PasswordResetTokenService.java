package congtuong.dev.cinemabooking.security.jwt;

import congtuong.dev.cinemabooking.exception.UserSecurityException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {
    private static final String KEY_PREFIX = "security:password-reset:";
    private static final Duration TTL = Duration.ofMinutes(15);

    private final StringRedisTemplate redisTemplate;
    private final TokenHashService tokenHashService;

    public String issue(UUID userId) {
        String token = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(
                key(token),
                userId.toString(),
                TTL
        );
        return token;
    }

    public UUID consume(String token) {
        String value = redisTemplate.opsForValue().getAndDelete(key(token));
        if (value == null) {
            throw new UserSecurityException(
                    HttpStatus.BAD_REQUEST,
                    "Password reset token is invalid or expired"
            );
        }
        return UUID.fromString(value);
    }

    private String key(String token) {
        return KEY_PREFIX + tokenHashService.hash(token);
    }
}
