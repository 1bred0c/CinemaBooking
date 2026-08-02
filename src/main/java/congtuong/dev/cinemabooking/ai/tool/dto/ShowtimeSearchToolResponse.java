package congtuong.dev.cinemabooking.ai.tool.dto;

import java.util.List;

public record ShowtimeSearchToolResponse(
        boolean success,
        String message,
        List<ShowtimeToolItem> showtimes
) {
}
