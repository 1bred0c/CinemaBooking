package congtuong.dev.cinemabooking.ai.tool.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ShowtimeToolItem(
        UUID showtimeId,
        UUID movieId,
        String movieTitle,
        UUID cinemaId,
        String cinemaName,
        String cinemaAddress,
        UUID roomId,
        String roomName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal basePrice,
        long availableSeats
) {
}
