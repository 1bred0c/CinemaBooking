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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;

@RestController
@RequestMapping("/api/v1/ai/chat")
@RequiredArgsConstructor
@Validated
public class AiChatController {

    private final AiChatService aiChatService;
    private final ConversationMemoryService conversationMemoryService;

    @GetMapping("/history")
    public ChatHistoryResponse history(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @RequestParam(required = false) @Positive Long beforeSequence,
            @RequestParam(defaultValue = "30")
            @Min(1) @Max(100) int limit
    ) {
        return conversationMemoryService.getHistory(
                currentUser.getUser().getId(),
                beforeSequence,
                limit
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
