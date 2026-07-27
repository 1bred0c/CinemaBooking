package congtuong.dev.cinemabooking.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record GenreResponse(
        UUID id,
        String name,
        String description,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
