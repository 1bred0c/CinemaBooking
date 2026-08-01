package congtuong.dev.cinemabooking.ai.chat;

import congtuong.dev.cinemabooking.ai.chat.dto.ChatRequest;
import congtuong.dev.cinemabooking.ai.chat.dto.ChatResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return aiChatService.chat(request.message());
    }
}
