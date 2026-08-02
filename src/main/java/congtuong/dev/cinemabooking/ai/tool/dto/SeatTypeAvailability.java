package congtuong.dev.cinemabooking.ai.tool.dto;

import congtuong.dev.cinemabooking.entity.enums.SeatType;

import java.math.BigDecimal;

public record SeatTypeAvailability(
        SeatType seatType,
        long totalSeats,
        long availableSeats,
        BigDecimal minimumPrice,
        BigDecimal maximumPrice
) {
}
