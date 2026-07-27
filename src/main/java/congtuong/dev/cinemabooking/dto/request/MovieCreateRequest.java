package congtuong.dev.cinemabooking.dto.request;

import congtuong.dev.cinemabooking.entity.enums.AgeRating;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record MovieCreateRequest(
        @NotBlank(message = "Title must not be blank")
        String title,
        String description,
        @NotNull(message = "Duration minutes must not be null")
        @Positive(message = "Duration minutes must be positive")
        Integer durationMinutes,
        LocalDate releaseDate,
        String director,
        String posterUrl,
        String trailerUrl,
        AgeRating ageRating
) {
}
