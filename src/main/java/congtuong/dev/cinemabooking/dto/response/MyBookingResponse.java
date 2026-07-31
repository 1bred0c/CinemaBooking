package congtuong.dev.cinemabooking.dto.response;

import congtuong.dev.cinemabooking.entity.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record MyBookingResponse(
        UUID id,
        BookingStatus status,
        BigDecimal totalAmount,
        UUID showtimeId,
        LocalDateTime showtimeStart,
        LocalDateTime showtimeEnd,
        UUID movieId,
        String movieTitle,
        String moviePosterUrl,
        UUID cinemaId,
        String cinemaName,
        String cinemaAddress,
        UUID roomId,
        String roomName,
        Instant paymentExpiresAt,
        Instant confirmedAt,
        Instant cancelledAt,
        Instant createdAt
) {
}
