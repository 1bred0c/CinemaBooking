package congtuong.dev.cinemabooking.dto.request;

import congtuong.dev.cinemabooking.entity.enums.SeatType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record SeatCreateRequest(
        @NotNull(message = "Room ID is required")
        UUID roomId,

        @NotBlank(message = "Seat row is required")
        String row,

        @NotNull(message = "Seat number is required")
        @Min(value = 1, message = "Seat number must be positive")
        Integer number,

        @NotNull(message = "Seat type is required")
        SeatType type
) {
}
