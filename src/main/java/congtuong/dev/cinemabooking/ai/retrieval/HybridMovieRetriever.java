package congtuong.dev.cinemabooking.ai.retrieval;

import congtuong.dev.cinemabooking.ai.query.MovieSearchPlan;

import java.util.List;

public interface HybridMovieRetriever {
    List<MovieCandidate> search(MovieSearchPlan plan);
}
