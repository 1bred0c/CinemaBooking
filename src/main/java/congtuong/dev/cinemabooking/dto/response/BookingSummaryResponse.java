package congtuong.dev.cinemabooking.dto.response;

import congtuong.dev.cinemabooking.entity.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record BookingSummaryResponse(
        UUID id,
        UUID showtimeId,
        BookingStatus status,
        BigDecimal totalAmount,
        Instant paymentExpiresAt,
        Instant createdAt
) {
}
