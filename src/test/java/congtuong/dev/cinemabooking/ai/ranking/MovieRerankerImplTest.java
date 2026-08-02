package congtuong.dev.cinemabooking.ai.ranking;

import congtuong.dev.cinemabooking.ai.query.MovieSearchPlan;
import congtuong.dev.cinemabooking.ai.rag.config.MovieRagProperties;
import congtuong.dev.cinemabooking.ai.retrieval.MovieCandidate;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class MovieRerankerImplTest {

    @Test
    void disabledRerankerUsesRrfOrderWithoutCallingModel() {
        ChatClient chatClient = mock(ChatClient.class);
        MovieReranker reranker = new MovieRerankerImpl(
                chatClient,
                new MovieRagProperties(2, 0, 10, 60, false)
        );
        MovieCandidate first = candidate("The Martian", 0.04);
        MovieCandidate second = candidate("Interstellar", 0.02);

        List<RankedMovie> result = reranker.rerank(
                "sinh tồn ngoài không gian",
                new MovieSearchPlan(
                        "sinh tồn ngoài không gian", null, null, null,
                        List.of(), null, null, null
                ),
                List.of(first, second)
        );

        assertEquals(List.of("The Martian", "Interstellar"),
                result.stream().map(RankedMovie::title).toList());
        assertEquals(1.0, result.get(0).relevanceScore());
        verifyNoInteractions(chatClient);
    }

    private MovieCandidate candidate(String title, double score) {
        return new MovieCandidate(
                UUID.randomUUID(), title, "Movie title: " + title,
                score, 0, List.of()
        );
    }
}
