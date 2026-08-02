package congtuong.dev.cinemabooking.ai.memory.dto;

import congtuong.dev.cinemabooking.ai.memory.ChatMessageRole;

import java.time.Instant;
import java.util.UUID;

public record ChatHistoryMessageResponse(
        UUID id,
        ChatMessageRole role,
        String content,
        Instant createdAt
) {
}
