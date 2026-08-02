package congtuong.dev.cinemabooking.ai.query;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatQueryAnalyzerImplTest {

    @Test
    void zeroViewerAgeMeansNoAgeConstraint() {
        ChatQueryAnalyzerImpl analyzer = new ChatQueryAnalyzerImpl(mock(ChatClient.class));

        assertNull(analyzer.validAgeOrNull(0));
        assertNull(analyzer.validAgeOrNull(null));
        assertEquals(13, analyzer.validAgeOrNull(13));
    }

    @Test
    void providerFailureFallsBackToRawSemanticMovieSearch() {
        ChatClient chatClient = mock(ChatClient.class);
        when(chatClient.prompt()).thenThrow(new IllegalStateException("down"));
        ChatQueryAnalyzer analyzer = new ChatQueryAnalyzerImpl(chatClient);

        ChatQueryPlan plan = analyzer.analyze(
                "phim sinh tồn trên sao Hỏa", ""
        );

        assertEquals(ChatIntent.MOVIE_SEARCH, plan.intent());
        assertEquals(0.0, plan.confidence());
        assertEquals(
                "phim sinh tồn trên sao Hỏa",
                plan.movieSearch().semanticQuery()
        );
        assertTrue(plan.movieSearch().genres().isEmpty());
    }
}
