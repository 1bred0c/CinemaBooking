package congtuong.dev.cinemabooking.dto.response;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TicketResponse(
        UUID bookingId,
        String bookingCode,
        String movieTitle,
        String cinemaName,
        String roomName,
        LocalDateTime showtimeStart,
        List<String> seats,
        String qrToken,
        Instant checkedInAt
) {
}
