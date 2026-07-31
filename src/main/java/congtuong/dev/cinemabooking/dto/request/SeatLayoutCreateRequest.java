package congtuong.dev.cinemabooking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record SeatLayoutCreateRequest(
        @NotNull(message = "Room ID is required")
        UUID roomId,

        @NotEmpty(message = "Seat layout must contain at least one row")
        List<@Valid SeatRowCreateRequest> rows
) {
}
