package congtuong.dev.cinemabooking.ai.chat.dto;

import java.util.UUID;

public record MovieSourceResponse(
        UUID movieId,
        String title,
        Double score
) {
}
