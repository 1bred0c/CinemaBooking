package congtuong.dev.cinemabooking.dto.response;

import congtuong.dev.cinemabooking.entity.enums.RoomType;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record ShowtimeBrowseResponse(
        UUID id,
        UUID movieId,
        String movieTitle,
        UUID cinemaId,
        String cinemaName,
        String cinemaAddress,
        UUID roomId,
        String roomName,
        RoomType roomType,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal basePrice,
        long availableSeats
) {
}
