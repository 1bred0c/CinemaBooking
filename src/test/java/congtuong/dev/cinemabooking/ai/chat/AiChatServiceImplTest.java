package congtuong.dev.cinemabooking.ai.chat;

import congtuong.dev.cinemabooking.ai.chat.dto.ChatResponse;
import congtuong.dev.cinemabooking.ai.chat.exception.AiChatException;
import congtuong.dev.cinemabooking.ai.query.ChatIntent;
import congtuong.dev.cinemabooking.ai.query.ChatQueryAnalyzer;
import congtuong.dev.cinemabooking.ai.query.ChatQueryPlan;
import congtuong.dev.cinemabooking.ai.query.MovieSearchPlan;
import congtuong.dev.cinemabooking.ai.ranking.MovieReranker;
import congtuong.dev.cinemabooking.ai.ranking.RankedMovie;
import congtuong.dev.cinemabooking.ai.retrieval.HybridMovieRetriever;
import congtuong.dev.cinemabooking.ai.retrieval.MovieCandidate;
import congtuong.dev.cinemabooking.ai.tool.CinemaBookingTools;
import congtuong.dev.cinemabooking.ai.memory.ConversationMemoryContext;
import congtuong.dev.cinemabooking.ai.memory.ConversationMemoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock private ChatClient chatClient;
    @Mock private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock private ChatClient.CallResponseSpec responseSpec;
    @Mock private ChatQueryAnalyzer queryAnalyzer;
    @Mock private HybridMovieRetriever hybridMovieRetriever;
    @Mock private MovieReranker movieReranker;
    @Mock private CinemaBookingTools cinemaBookingTools;
    @Mock private ConversationMemoryService conversationMemoryService;

    @Test
    void chatReturnsGroundedContentFromFinalRerankedSources() {
        AiChatService service = service();
        String message = "Gợi ý một bộ phim";
        MovieSearchPlan searchPlan = searchPlan(message);
        UUID movieId = UUID.randomUUID();
        MovieCandidate candidate = new MovieCandidate(
                movieId, "Interstellar", "Movie title: Interstellar",
                0.03, 0, List.of()
        );
        RankedMovie ranked = new RankedMovie(
                movieId, "Interstellar", candidate.content(),
                0.95, "Phù hợp nội dung khám phá không gian"
        );
        memoryIsEmpty();
        when(queryAnalyzer.analyze(message, "")).thenReturn(new ChatQueryPlan(
                ChatIntent.MOVIE_SEARCH, 0.9, searchPlan
        ));
        when(hybridMovieRetriever.search(searchPlan))
                .thenReturn(List.of(candidate));
        when(movieReranker.rerank(message, searchPlan, List.of(candidate)))
                .thenReturn(List.of(ranked));
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(message)).thenReturn(requestSpec);
        when(requestSpec.options(any(OpenAiChatOptions.Builder.class)))
                .thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("Bạn có thể xem Interstellar.");

        ChatResponse response = service.chat(USER_ID, message);

        assertEquals("Bạn có thể xem Interstellar.", response.message());
        assertEquals(movieId, response.sources().get(0).movieId());
        assertEquals(0.95, response.sources().get(0).score());
    }

    @Test
    void liveDataIntentUsesToolsWithoutRunningRag() {
        AiChatService service = service();
        String message = "Interstellar tối nay có suất nào?";
        memoryIsEmpty();
        when(queryAnalyzer.analyze(message, "")).thenReturn(new ChatQueryPlan(
                ChatIntent.LIVE_DATA, 0.95, null
        ));
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(any(String.class))).thenReturn(requestSpec);
        when(requestSpec.user(message)).thenReturn(requestSpec);
        when(requestSpec.options(any(OpenAiChatOptions.Builder.class)))
                .thenReturn(requestSpec);
        when(requestSpec.tools(any(Object[].class))).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("Có suất lúc 19:30.");

        ChatResponse response = service.chat(USER_ID, message);

        assertEquals("Có suất lúc 19:30.", response.message());
        assertEquals(List.of(), response.sources());
        ArgumentCaptor<OpenAiChatOptions.Builder> optionsCaptor =
                ArgumentCaptor.forClass(OpenAiChatOptions.Builder.class);
        verify(requestSpec).options(optionsCaptor.capture());
        assertEquals(
                "none",
                optionsCaptor.getValue().build().getReasoningEffort()
        );
        verifyNoInteractions(hybridMovieRetriever, movieReranker);
    }

    @Test
    void greetingDoesNotSearchOrCallGenerationModel() {
        AiChatService service = service();
        memoryIsEmpty();
        when(queryAnalyzer.analyze("Xin chào", "")).thenReturn(new ChatQueryPlan(
                ChatIntent.GREETING, 1.0, null
        ));

        ChatResponse response = service.chat(USER_ID, "Xin chào");

        assertEquals(
                "Xin chào! Mình có thể giúp bạn tìm và khám phá phim.",
                response.message()
        );
        verifyNoInteractions(hybridMovieRetriever, movieReranker, chatClient);
    }

    @Test
    void chatMapsProviderFailureToDomainException() {
        AiChatService service = service();
        String message = "Tìm phim không gian";
        MovieSearchPlan plan = searchPlan(message);
        MovieCandidate candidate = new MovieCandidate(
                UUID.randomUUID(), "Dune", "Movie title: Dune", 0.03, 0, List.of()
        );
        RankedMovie ranked = new RankedMovie(
                candidate.movieId(), candidate.title(), candidate.content(),
                0.8, "Phim khoa học viễn tưởng"
        );
        memoryIsEmpty();
        when(queryAnalyzer.analyze(message, "")).thenReturn(new ChatQueryPlan(
                ChatIntent.MOVIE_SEARCH, 0.8, plan
        ));
        when(hybridMovieRetriever.search(plan)).thenReturn(List.of(candidate));
        when(movieReranker.rerank(message, plan, List.of(candidate)))
                .thenReturn(List.of(ranked));
        when(chatClient.prompt()).thenThrow(new IllegalStateException("down"));

        AiChatException exception = assertThrows(
                AiChatException.class,
                () -> service.chat(USER_ID, message)
        );

        assertEquals("AI assistant is temporarily unavailable", exception.getMessage());
    }

    @Test
    void chatDoesNotCallGenerationModelWhenHybridSearchIsEmpty() {
        AiChatService service = service();
        String message = "phim không tồn tại";
        MovieSearchPlan plan = searchPlan(message);
        memoryIsEmpty();
        when(queryAnalyzer.analyze(message, "")).thenReturn(new ChatQueryPlan(
                ChatIntent.MOVIE_SEARCH, 0.7, plan
        ));
        when(hybridMovieRetriever.search(plan)).thenReturn(List.of());
        when(movieReranker.rerank(message, plan, List.of()))
                .thenReturn(List.of());

        ChatResponse response = service.chat(USER_ID, message);

        assertEquals(
                "Mình chưa tìm thấy phim phù hợp trong dữ liệu CinemaBooking.",
                response.message()
        );
        verifyNoInteractions(chatClient);
    }

    private AiChatService service() {
        return new AiChatServiceImpl(
                chatClient,
                queryAnalyzer,
                hybridMovieRetriever,
                movieReranker,
                cinemaBookingTools,
                conversationMemoryService
        );
    }

    private void memoryIsEmpty() {
        when(conversationMemoryService.load(USER_ID))
                .thenReturn(ConversationMemoryContext.empty());
    }

    private MovieSearchPlan searchPlan(String query) {
        return new MovieSearchPlan(
                query, query, null, null, List.of(),
                null, null, null
        );
    }
}
