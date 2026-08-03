package congtuong.dev.cinemabooking.dto.request;

import congtuong.dev.cinemabooking.entity.enums.ShowtimeStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record ShowtimeFilterRequest(
        UUID movieId,
        UUID cinemaId,
        UUID roomId,
        LocalDateTime startTimeFrom,
        LocalDateTime startTimeTo,
        ShowtimeStatus status,
        Boolean active
) {
}
