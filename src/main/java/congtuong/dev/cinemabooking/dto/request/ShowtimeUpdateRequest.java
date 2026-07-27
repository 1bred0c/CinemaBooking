package congtuong.dev.cinemabooking.dto.request;

import congtuong.dev.cinemabooking.entity.enums.ShowtimeStatus;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ShowtimeUpdateRequest(
        UUID movieId,
        UUID roomId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        @Positive BigDecimal basePrice,
        ShowtimeStatus status
) {
}
