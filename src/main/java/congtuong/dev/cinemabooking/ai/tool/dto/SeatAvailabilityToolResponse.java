package congtuong.dev.cinemabooking.ai.tool.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record SeatAvailabilityToolResponse(
        boolean success,
        String message,
        UUID showtimeId,
        String movieTitle,
        LocalDateTime startTime,
        long totalSeats,
        long availableSeats,
        long heldSeats,
        long bookedSeats,
        long blockedSeats,
        List<SeatTypeAvailability> bySeatType
) {
}
