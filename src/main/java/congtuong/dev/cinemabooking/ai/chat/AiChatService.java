package congtuong.dev.cinemabooking.ai.chat;

import congtuong.dev.cinemabooking.ai.chat.dto.ChatResponse;

import java.util.UUID;

public interface AiChatService {
    ChatResponse chat(UUID userId, String message);
}
