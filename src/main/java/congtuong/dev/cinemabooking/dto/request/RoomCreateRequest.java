package congtuong.dev.cinemabooking.dto.request;

import congtuong.dev.cinemabooking.entity.enums.RoomType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record RoomCreateRequest(
        @NotBlank(message = "Room name is required")
        String name,

        @NotNull(message = "Total rows is required")
        @Min(value = 1, message = "Total rows must be positive")
        Integer totalRows,

        @NotNull(message = "Room type is required")
        RoomType roomType,

        @NotNull(message = "Cinema ID is required")
        UUID cinemaId
) {
}
