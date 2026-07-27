package congtuong.dev.cinemabooking.dto.response;

import java.time.LocalDateTime;

public record ErrorRespone(
        int status,
        String message,
        LocalDateTime timestamp
) {
}
