package congtuong.dev.cinemabooking.ai.chat;

import congtuong.dev.cinemabooking.ai.chat.dto.ChatResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {
        "spring.ai.model.chat=openai",
        "booking.expiration-enabled=false"
})
@EnabledIfEnvironmentVariable(
        named = "RUN_OPENAI_LIVE_TEST",
        matches = "true"
)
class AiChatLiveTest {

    @DynamicPropertySource
    static void openAiProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.ai.openai.api-key",
                () -> System.getenv("OPENAI_API_KEY")
        );
    }

    @Autowired
    private AiChatService aiChatService;

    @Test
    void returnsARealOpenAiResponse() {
        ChatResponse response = aiChatService.chat(
                "Reply with exactly: CINEMA_AI_OK"
        );

        assertTrue(response.message().contains("CINEMA_AI_OK"));
    }
}
