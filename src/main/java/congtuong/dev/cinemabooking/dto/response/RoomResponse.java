package congtuong.dev.cinemabooking.dto.response;

import congtuong.dev.cinemabooking.entity.enums.RoomStatus;
import congtuong.dev.cinemabooking.entity.enums.RoomType;

import java.util.UUID;

public record RoomResponse(
        UUID id,
        String name,
        Integer totalSeats,
        Integer totalRows,
        Integer totalColumns,
        RoomType roomType,
        RoomStatus status,
        UUID cinemaId) {
}
