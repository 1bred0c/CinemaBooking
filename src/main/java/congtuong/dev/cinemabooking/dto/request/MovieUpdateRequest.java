package congtuong.dev.cinemabooking.dto.request;

import congtuong.dev.cinemabooking.entity.enums.AgeRating;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record MovieUpdateRequest(
        @Pattern(regexp = ".*\\S.*", message = "Title must not be blank")
        String title,
        String description,
        @Positive(message = "Duration minutes must be positive")
        Integer durationMinutes,
        LocalDate releaseDate,
        String director,
        String posterUrl,
        String trailerUrl,
        AgeRating ageRating
) {
}
