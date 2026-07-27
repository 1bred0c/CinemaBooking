package congtuong.dev.cinemabooking.dto.response;

import congtuong.dev.cinemabooking.entity.enums.ShowtimeStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ShowtimeResponse(
        UUID id,
        UUID movieId,
        String movieTitle,
        UUID roomId,
        String roomName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal basePrice,
        ShowtimeStatus status,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
