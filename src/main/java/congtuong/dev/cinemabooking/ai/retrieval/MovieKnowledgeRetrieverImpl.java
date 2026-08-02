package congtuong.dev.cinemabooking.ai.retrieval;

import congtuong.dev.cinemabooking.ai.rag.config.MovieRagProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStoreRetriever;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MovieKnowledgeRetrieverImpl implements MovieKnowledgeRetriever {

    private final VectorStoreRetriever vectorStoreRetriever;
    private final MovieRagProperties properties;

    @Override
    public List<MovieSearchResult> search(String query) {
        return search(
                query,
                properties.topK(),
                properties.similarityThreshold()
        );
    }

    @Override
    public List<MovieSearchResult> search(
            String query,
            int topK,
            double similarityThreshold
    ) {
        SearchRequest request = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .similarityThreshold(similarityThreshold)
                .filterExpression("active == true && sourceType == 'MOVIE'")
                .build();

        return vectorStoreRetriever.similaritySearch(request).stream()
                .map(this::toResult)
                .toList();
    }

    private MovieSearchResult toResult(Document document) {
        return new MovieSearchResult(
                UUID.fromString(document.getMetadata().get("movieId").toString()),
                document.getMetadata().get("title").toString(),
                document.getText(),
                document.getScore()
        );
    }
}
