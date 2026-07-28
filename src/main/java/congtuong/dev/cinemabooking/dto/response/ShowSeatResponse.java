package congtuong.dev.cinemabooking.dto.response;

import congtuong.dev.cinemabooking.entity.enums.SeatType;
import congtuong.dev.cinemabooking.entity.enums.ShowSeatStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record ShowSeatResponse(
        UUID id,
        UUID showtimeId,
        UUID seatId,
        String seatRow,
        Integer seatNumber,
        SeatType seatType,
        ShowSeatStatus status,
        BigDecimal price
) {
}
