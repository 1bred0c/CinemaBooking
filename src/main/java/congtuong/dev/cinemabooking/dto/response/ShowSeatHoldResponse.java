package congtuong.dev.cinemabooking.dto.response;

import congtuong.dev.cinemabooking.entity.enums.ShowSeatHoldStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ShowSeatHoldResponse(
        UUID holdId,
        UUID showtimeId,
        ShowSeatHoldStatus status,
        Instant expiresAt,
        BigDecimal totalAmount,
        List<ShowSeatHoldItemResponse> seats
) {
}
