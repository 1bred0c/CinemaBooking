package congtuong.dev.cinemabooking.dto.response;

import congtuong.dev.cinemabooking.entity.enums.BookingStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BookingResponse(
        UUID id,
        UUID userId,
        UUID showtimeId,
        UUID holdId,
        BookingStatus status,
        BigDecimal totalAmount,
        Instant paymentExpiresAt,
        Instant confirmedAt,
        Instant cancelledAt,
        Instant createdAt,
        List<BookingItemResponse> items
) {
}
