package congtuong.dev.cinemabooking.ai.retrieval;

import java.util.List;

public interface MovieKnowledgeRetriever {
    List<MovieSearchResult> search(String query);
}
