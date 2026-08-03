package congtuong.dev.cinemabooking.dto.request;

import congtuong.dev.cinemabooking.entity.enums.AgeRating;

import java.util.UUID;

public record MovieFilterRequest(
        String keyword,
        UUID genreId,
        AgeRating ageRating,
        Boolean active
) {
}
