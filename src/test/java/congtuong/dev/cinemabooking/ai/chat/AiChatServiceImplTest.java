package congtuong.dev.cinemabooking.ai.chat;

import congtuong.dev.cinemabooking.ai.chat.dto.ChatResponse;
import congtuong.dev.cinemabooking.ai.chat.exception.AiChatException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec responseSpec;

    @Test
    void chatReturnsProviderContent() {
        AiChatService service = new AiChatServiceImpl(chatClient);
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user("Gợi ý một bộ phim")).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("Bạn có thể xem Interstellar.");

        ChatResponse response = service.chat("Gợi ý một bộ phim");

        assertEquals("Bạn có thể xem Interstellar.", response.message());
    }

    @Test
    void chatMapsProviderFailureToDomainException() {
        AiChatService service = new AiChatServiceImpl(chatClient);
        when(chatClient.prompt()).thenThrow(new IllegalStateException("down"));

        AiChatException exception = assertThrows(
                AiChatException.class,
                () -> service.chat("Xin chào")
        );

        assertEquals(
                "AI assistant is temporarily unavailable",
                exception.getMessage()
        );
    }
}
