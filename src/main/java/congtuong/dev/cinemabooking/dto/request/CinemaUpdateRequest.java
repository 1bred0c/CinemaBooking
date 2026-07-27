package congtuong.dev.cinemabooking.dto.request;

import jakarta.validation.constraints.Size;

public record CinemaUpdateRequest (
        @Size(min = 1, max = 50)
        String name,
        @Size(min = 1, max = 50)
        String address
) {
}
