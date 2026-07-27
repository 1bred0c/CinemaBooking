package congtuong.dev.cinemabooking.dto.request;

import congtuong.dev.cinemabooking.entity.enums.SeatType;

import java.util.UUID;

public record SeatUpdateRequest(
        UUID id,
        UUID roomId,
        String row,
        Integer number,
        SeatType type
) {
}
