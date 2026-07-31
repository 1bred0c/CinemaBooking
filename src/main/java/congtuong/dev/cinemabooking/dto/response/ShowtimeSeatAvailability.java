package congtuong.dev.cinemabooking.dto.response;

import java.util.UUID;

public record ShowtimeSeatAvailability(
        UUID showtimeId,
        long availableSeats
) {
}
