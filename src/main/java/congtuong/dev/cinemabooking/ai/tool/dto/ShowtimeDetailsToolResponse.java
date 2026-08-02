package congtuong.dev.cinemabooking.ai.tool.dto;

public record ShowtimeDetailsToolResponse(
        boolean success,
        String message,
        ShowtimeToolItem showtime
) {
}
