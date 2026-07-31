package congtuong.dev.cinemabooking.dto.response;

import java.util.UUID;

public record MoviePosterResponse(
        UUID movieId,
        String posterUrl,
        String publicId
) {
}
