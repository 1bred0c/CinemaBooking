package congtuong.dev.cinemabooking.dto.request;

import jakarta.validation.constraints.Pattern;

public record GenreUpdateRequest(
        @Pattern(regexp = ".*\\S.*", message = "Name must not be blank")
        String name,
        String description
) {
}
