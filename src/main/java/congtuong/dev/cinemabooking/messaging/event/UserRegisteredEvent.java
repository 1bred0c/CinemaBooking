package congtuong.dev.cinemabooking.messaging.event;

import congtuong.dev.cinemabooking.entity.User;

import java.time.Instant;
import java.util.UUID;

public record UserRegisteredEvent(
        UUID eventId,
        UUID userId,
        String email,
        String fullName,
        Instant occurredAt,
        int version
) {
    public static UserRegisteredEvent from(User user) {
        return new UserRegisteredEvent(
                UUID.randomUUID(),
                user.getId(),
                user.getEmail(),
                user.getFullname(),
                Instant.now(),
                1
        );
    }
}
