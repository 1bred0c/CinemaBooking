package congtuong.dev.cinemabooking.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ShowSeatHoldCreateRequest(
        @NotEmpty(message = "At least one show seat must be selected")
        List<@NotNull(message = "Show seat ID must not be null") UUID> showSeatIds
) {
}
