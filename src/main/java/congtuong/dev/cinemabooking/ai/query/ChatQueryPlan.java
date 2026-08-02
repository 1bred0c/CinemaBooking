package congtuong.dev.cinemabooking.ai.query;

public record ChatQueryPlan(
        ChatIntent intent,
        Double confidence,
        MovieSearchPlan movieSearch
) {
    public boolean requiresMovieSearch() {
        return intent == ChatIntent.MOVIE_SEARCH
                || intent == ChatIntent.MOVIE_INFORMATION
                || intent == ChatIntent.MOVIE_SEARCH_WITH_LIVE_DATA;
    }
}
