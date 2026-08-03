package congtuong.dev.cinemabooking.ai.memory.dto;

import java.util.List;
import java.util.UUID;

public record ChatHistoryResponse(
        UUID conversationId,
        List<ChatHistoryMessageResponse> messages,
        Long nextCursor,
        boolean hasMore
) {
    public static ChatHistoryResponse empty() {
        return new ChatHistoryResponse(null, List.of(), null, false);
    }
}
