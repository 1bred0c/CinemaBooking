package congtuong.dev.cinemabooking.dto.response;

import congtuong.dev.cinemabooking.entity.enums.SeatType;

import java.math.BigDecimal;
import java.util.UUID;

public record BookingItemResponse(
        UUID id,
        UUID showSeatId,
        String seatRow,
        Integer seatNumber,
        SeatType seatType,
        BigDecimal unitPrice
) {
}
