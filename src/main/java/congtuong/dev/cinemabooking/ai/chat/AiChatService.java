package congtuong.dev.cinemabooking.ai.chat;

import congtuong.dev.cinemabooking.ai.chat.dto.ChatResponse;

public interface AiChatService {
    ChatResponse chat(String message);
}
