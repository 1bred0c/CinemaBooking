package congtuong.dev.cinemabooking.ai.ranking;

import congtuong.dev.cinemabooking.ai.query.MovieSearchPlan;
import congtuong.dev.cinemabooking.ai.retrieval.MovieCandidate;

import java.util.List;

public interface MovieReranker {
    List<RankedMovie> rerank(
            String userMessage,
            MovieSearchPlan plan,
            List<MovieCandidate> candidates
    );
}
