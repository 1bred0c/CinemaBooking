package congtuong.dev.cinemabooking.dto.request;

import congtuong.dev.cinemabooking.entity.enums.SeatType;

import java.util.UUID;

public record SeatUpdateRequest(
        UUID roomId,
        String row,
        Integer number,
        SeatType type
) {
}
