package congtuong.dev.cinemabooking.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record BookingCreateRequest(
        @NotNull(message = "Hold ID is required")
        UUID holdId
) {
}
