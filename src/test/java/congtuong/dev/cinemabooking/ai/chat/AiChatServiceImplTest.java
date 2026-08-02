package congtuong.dev.cinemabooking.ai.chat;

import congtuong.dev.cinemabooking.ai.chat.dto.ChatResponse;
import congtuong.dev.cinemabooking.ai.chat.exception.AiChatException;
import congtuong.dev.cinemabooking.ai.retrieval.MovieKnowledgeRetriever;
import congtuong.dev.cinemabooking.ai.retrieval.MovieSearchResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.CallResponseSpec responseSpec;
    @Mock
    private MovieKnowledgeRetriever movieKnowledgeRetriever;

    @Test
    void chatReturnsGroundedProviderContentAndSources() {
        AiChatService service = new AiChatServiceImpl(
                chatClient,
                movieKnowledgeRetriever
        );
        UUID movieId = UUID.randomUUID();
        when(movieKnowledgeRetriever.search("Gợi ý một bộ phim"))
                .thenReturn(List.of(new MovieSearchResult(
                        movieId,
                        "Interstellar",
                        "Movie title: Interstellar",
                        0.91
                )));
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user("Gợi ý một bộ phim")).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn(
                "Bạn có thể xem Interstellar."
        );

        ChatResponse response = service.chat("Gợi ý một bộ phim");

        assertEquals("Bạn có thể xem Interstellar.", response.message());
        assertEquals(movieId, response.sources().get(0).movieId());
    }

    @Test
    void chatMapsProviderFailureToDomainException() {
        AiChatService service = new AiChatServiceImpl(
                chatClient,
                movieKnowledgeRetriever
        );
        when(movieKnowledgeRetriever.search("Xin chào"))
                .thenReturn(List.of(new MovieSearchResult(
                        UUID.randomUUID(),
                        "Dune",
                        "Movie title: Dune",
                        0.80
                )));
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

    @Test
    void chatDoesNotCallProviderWhenNoMovieMatches() {
        AiChatService service = new AiChatServiceImpl(
                chatClient,
                movieKnowledgeRetriever
        );
        when(movieKnowledgeRetriever.search("phim không tồn tại"))
                .thenReturn(List.of());

        ChatResponse response = service.chat("phim không tồn tại");

        assertEquals(
                "Mình chưa tìm thấy phim phù hợp trong dữ liệu CinemaBooking.",
                response.message()
        );
        assertEquals(List.of(), response.sources());
        verifyNoInteractions(chatClient);
    }
}
