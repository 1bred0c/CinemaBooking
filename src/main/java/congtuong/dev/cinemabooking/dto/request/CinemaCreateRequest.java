package congtuong.dev.cinemabooking.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CinemaCreateRequest(
        @NotBlank(message = "Name must not be null")
        @Size(min = 1, max = 50)
        String name,

        @NotBlank(message = "Address must not be null")
        @Size(min = 1, max = 50)
        String address
) {
}
