package congtuong.dev.cinemabooking.dto.request;

import congtuong.dev.cinemabooking.entity.enums.ShowtimeStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ShowtimeCreateRequest(
        @NotNull UUID movieId,
        @NotNull UUID roomId,
        @NotNull LocalDateTime startTime,
        @NotNull @Positive BigDecimal basePrice,
        ShowtimeStatus status
) {
}
