package congtuong.dev.cinemabooking.ai.retrieval;

import congtuong.dev.cinemabooking.ai.rag.config.MovieRagProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStoreRetriever;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieKnowledgeRetrieverImplTest {

    @Mock
    private VectorStoreRetriever vectorStoreRetriever;
    @Mock
    private Document document;

    @Test
    void searchesWithConfiguredLimitsAndMapsMovieMetadata() {
        UUID movieId = UUID.randomUUID();
        when(document.getMetadata()).thenReturn(Map.of(
                "movieId", movieId.toString(),
                "title", "Arrival"
        ));
        when(document.getText()).thenReturn("Movie title: Arrival");
        when(document.getScore()).thenReturn(0.88);
        when(vectorStoreRetriever.similaritySearch(
                org.mockito.ArgumentMatchers.any(SearchRequest.class)
        )).thenReturn(List.of(document));
        MovieKnowledgeRetriever retriever = new MovieKnowledgeRetrieverImpl(
                vectorStoreRetriever,
                new MovieRagProperties(4, 0.7, 10, 60, true)
        );

        List<MovieSearchResult> results = retriever.search("alien language");

        ArgumentCaptor<SearchRequest> requestCaptor =
                ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStoreRetriever).similaritySearch(requestCaptor.capture());
        assertEquals("alien language", requestCaptor.getValue().getQuery());
        assertEquals(4, requestCaptor.getValue().getTopK());
        assertEquals(0.7, requestCaptor.getValue().getSimilarityThreshold());
        assertEquals(movieId, results.get(0).movieId());
        assertEquals("Arrival", results.get(0).title());
        assertEquals(0.88, results.get(0).score());
    }
}
