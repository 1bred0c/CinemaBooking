package congtuong.dev.cinemabooking.dto.request;

import congtuong.dev.cinemabooking.entity.enums.RoomStatus;
import congtuong.dev.cinemabooking.entity.enums.RoomType;
import jakarta.validation.constraints.Min;

import java.util.UUID;

public record RoomUpdateRequest(
        String name,

        @Min(value = 1, message = "Total rows must be positive")
        Integer totalRows,

        RoomType roomType,
        RoomStatus roomStatus,
        UUID cinemaId
) {
}
