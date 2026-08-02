package congtuong.dev.cinemabooking.ai.chat;

import congtuong.dev.cinemabooking.ai.chat.dto.ChatRequest;
import congtuong.dev.cinemabooking.ai.chat.dto.ChatResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import congtuong.dev.cinemabooking.security.jwt.CustomUserDetails;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import congtuong.dev.cinemabooking.ai.memory.ConversationMemoryService;
import congtuong.dev.cinemabooking.ai.memory.dto.ChatHistoryResponse;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;
    private final ConversationMemoryService conversationMemoryService;

    @GetMapping("/history")
    public ChatHistoryResponse history(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return conversationMemoryService.getHistory(
                currentUser.getUser().getId()
        );
    }

    @PostMapping
    public ChatResponse chat(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody ChatRequest request
    ) {
        return aiChatService.chat(currentUser.getUser().getId(), request.message());
    }
}
