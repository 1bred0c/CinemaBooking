package congtuong.dev.cinemabooking.dto.response;

import congtuong.dev.cinemabooking.entity.enums.SeatType;

import java.math.BigDecimal;
import java.util.UUID;

public record ShowSeatHoldItemResponse(
        UUID showSeatId,
        UUID seatId,
        String seatRow,
        Integer seatNumber,
        SeatType seatType,
        BigDecimal heldPrice
) {
}
