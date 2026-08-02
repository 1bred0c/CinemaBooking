package congtuong.dev.cinemabooking.ai.retrieval;

import java.util.List;

public interface MovieKnowledgeRetriever {
    List<MovieSearchResult> search(String query);

    List<MovieSearchResult> search(
            String query,
            int topK,
            double similarityThreshold
    );
}
