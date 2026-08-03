package congtuong.dev.cinemabooking.security.jwt;

import congtuong.dev.cinemabooking.entity.User;
import congtuong.dev.cinemabooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class RedisSecurityStateService implements SecurityStateService {
    private static final String KEY_PREFIX = "security:user:";
    private static final String TRANSITION_KEY_PREFIX = "security:transition:";
    private static final DefaultRedisScript<Long> MONOTONIC_WRITE_SCRIPT =
            new DefaultRedisScript<>("""
                    local current = redis.call('GET', KEYS[1])
                    if current then
                        local separator = string.find(current, ':')
                        local currentVersion = tonumber(string.sub(current, separator + 1))
                        local incomingVersion = tonumber(ARGV[2])
                        if incomingVersion < currentVersion then
                            return 0
                        end
                    end
                    redis.call('SET', KEYS[1], ARGV[1], 'EX', ARGV[3])
                    return 1
                    """, Long.class);
    private static final DefaultRedisScript<Long> COMPLETE_TRANSITION_SCRIPT =
            new DefaultRedisScript<>("""
                    if redis.call('GET', KEYS[2]) ~= ARGV[1] then
                        return 0
                    end
                    local current = redis.call('GET', KEYS[1])
                    if current then
                        local separator = string.find(current, ':')
                        local currentVersion = tonumber(string.sub(current, separator + 1))
                        local incomingVersion = tonumber(ARGV[3])
                        if incomingVersion > currentVersion then
                            redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[4])
                        end
                    else
                        redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[4])
                    end
                    redis.call('DEL', KEYS[2])
                    return 1
                    """, Long.class);
    private static final DefaultRedisScript<Long> CANCEL_TRANSITION_SCRIPT =
            new DefaultRedisScript<>("""
                    if redis.call('GET', KEYS[1]) == ARGV[1] then
                        return redis.call('DEL', KEYS[1])
                    end
                    return 0
                    """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final UserRepository userRepository;
    private final JwtProperties jwtProperties;

    @Override
    public SecurityState get(UUID userId) {
        String key = key(userId);
        try {
            String cached = redisTemplate.opsForValue().get(key);
            if (cached != null) {
                return decode(cached);
            }
        } catch (RuntimeException exception) {
            log.warn("Redis security-state read failed for userId={}", userId);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));
        SecurityState state = from(user);
        writeBestEffort(key, state);
        return state;
    }

    @Override
    public void cache(User user) {
        writeBestEffort(key(user.getId()), from(user));
    }

    @Override
    @Transactional
    public void advanceVersion(User user) {
        user.setSecurityVersion(user.getSecurityVersion() + 1L);
        cache(user);
    }

    @Override
    public String beginTransition(UUID userId) {
        String operationId = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                transitionKey(userId),
                operationId,
                securityStateTtl()
        );
        if (!Boolean.TRUE.equals(acquired)) {
            throw new IllegalStateException("User security state is already being updated");
        }
        return operationId;
    }

    @Override
    public boolean isTransitioning(UUID userId) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(transitionKey(userId)));
        } catch (RuntimeException exception) {
            log.warn("Redis transition-state read failed for userId={}", userId);
            return true;
        }
    }

    @Override
    public void completeTransition(
            UUID userId,
            String operationId,
            SecurityState state
    ) {
        Long completed = redisTemplate.execute(
                COMPLETE_TRANSITION_SCRIPT,
                java.util.List.of(key(userId), transitionKey(userId)),
                operationId,
                encode(state),
                Long.toString(state.version()),
                Long.toString(securityStateTtl().toSeconds())
        );
        if (!Long.valueOf(1L).equals(completed)) {
            throw new IllegalStateException("Security transition ownership was lost");
        }
    }

    @Override
    public void cancelTransition(UUID userId, String operationId) {
        try {
            redisTemplate.execute(
                    CANCEL_TRANSITION_SCRIPT,
                    java.util.List.of(transitionKey(userId)),
                    operationId
            );
        } catch (RuntimeException exception) {
            log.warn("Failed to cancel security transition for userId={}", userId);
        }
    }

    private void writeBestEffort(String key, SecurityState state) {
        try {
            redisTemplate.execute(
                    MONOTONIC_WRITE_SCRIPT,
                    java.util.List.of(key),
                    encode(state),
                    Long.toString(state.version()),
                    Long.toString(securityStateTtl().toSeconds())
            );
        } catch (RuntimeException exception) {
            log.warn("Redis security-state write failed for key={}", key);
        }
    }

    private Duration securityStateTtl() {
        return jwtProperties.accessTokenExpiration();
    }

    private SecurityState from(User user) {
        return new SecurityState(user.isActive(), user.getSecurityVersion());
    }

    private String encode(SecurityState state) {
        return state.active() + ":" + state.version();
    }

    private SecurityState decode(String value) {
        String[] parts = value.split(":", 2);
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid cached security state");
        }
        return new SecurityState(
                Boolean.parseBoolean(parts[0]),
                Long.parseLong(parts[1])
        );
    }

    private String key(UUID userId) {
        return KEY_PREFIX + userId;
    }

    private String transitionKey(UUID userId) {
        return TRANSITION_KEY_PREFIX + userId;
    }
}
