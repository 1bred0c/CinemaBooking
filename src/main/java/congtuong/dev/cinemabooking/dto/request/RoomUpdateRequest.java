package congtuong.dev.cinemabooking.dto.request;

import congtuong.dev.cinemabooking.entity.enums.RoomStatus;
import congtuong.dev.cinemabooking.entity.enums.RoomType;

import java.util.UUID;

public record RoomUpdateRequest(
        String name,
        Integer totalSeats,
        Integer totalRows,
        Integer totalColumns,
        Integer totalPrice,
        RoomType roomType,
        RoomStatus roomStatus,
        UUID cinemaId
) {
}
