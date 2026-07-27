package congtuong.dev.cinemabooking.dto.request;

import congtuong.dev.cinemabooking.entity.enums.RoomType;

import java.util.UUID;

public record RoomCreateRequest(
        UUID id,
        String name,
        Integer totalSeats,
        Integer totalRows,
        Integer totalColumns,
        Integer totalPrice,
        RoomType roomType,
        UUID cinemaId
) {
}
