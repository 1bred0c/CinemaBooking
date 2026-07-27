package congtuong.dev.cinemabooking.dto.response;

import congtuong.dev.cinemabooking.entity.enums.AgeRating;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record MovieResponse(
        UUID id,
        String title,
        String description,
        Integer durationMinutes,
        LocalDate releaseDate,
        String director,
        String posterUrl,
        String trailerUrl,
        AgeRating ageRating,
        boolean active,
        List<GenreSummaryResponse> genres,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
