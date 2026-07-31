package congtuong.dev.cinemabooking.dto.request;

import congtuong.dev.cinemabooking.entity.enums.SeatType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SeatSectionCreateRequest(
        @NotNull(message = "Section start number is required")
        @Min(value = 1, message = "Section start number must be positive")
        Integer startNumber,

        @NotNull(message = "Section end number is required")
        @Min(value = 1, message = "Section end number must be positive")
        Integer endNumber,

        @NotNull(message = "Seat type is required")
        SeatType type
) {
}
