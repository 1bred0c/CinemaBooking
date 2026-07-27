package congtuong.dev.cinemabooking.dto.response;

import java.util.UUID;

public record CinemaResponse(
        UUID id,
        String name,
        String address,
        boolean isActive
) {
}
