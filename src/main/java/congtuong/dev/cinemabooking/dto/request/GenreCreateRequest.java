package congtuong.dev.cinemabooking.dto.request;

import jakarta.validation.constraints.NotBlank;

public record GenreCreateRequest(
        @NotBlank(message = "Name must not be blank")
        String name,
        String description
) {
}
