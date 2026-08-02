package congtuong.dev.cinemabooking.ai.tool.dto;

import congtuong.dev.cinemabooking.entity.enums.SeatType;

import java.math.BigDecimal;

public record SeatTypePrice(
        SeatType seatType,
        BigDecimal minimumPrice,
        BigDecimal maximumPrice
) {
}
