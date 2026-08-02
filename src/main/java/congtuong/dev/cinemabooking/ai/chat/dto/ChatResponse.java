package congtuong.dev.cinemabooking.ai.chat.dto;

import java.util.List;

public record ChatResponse(
        String message,
        List<MovieSourceResponse> sources
) {
    public ChatResponse(String message) {
        this(message, List.of());
    }
}
