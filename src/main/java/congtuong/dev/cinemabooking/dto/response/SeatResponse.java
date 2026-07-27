package congtuong.dev.cinemabooking.dto.response;

import congtuong.dev.cinemabooking.entity.enums.SeatType;

import java.util.UUID;

public record SeatResponse(
        UUID id,
        UUID roomId,
        String roomName,
        String row,
        Integer number,
        SeatType type,
        boolean isActive) {
}
