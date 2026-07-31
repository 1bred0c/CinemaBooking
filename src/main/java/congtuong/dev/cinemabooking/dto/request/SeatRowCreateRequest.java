package congtuong.dev.cinemabooking.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SeatRowCreateRequest(
        @NotBlank(message = "Seat row is required")
        String row,

        @NotEmpty(message = "A seat row must contain at least one section")
        List<@Valid SeatSectionCreateRequest> sections
) {
}
