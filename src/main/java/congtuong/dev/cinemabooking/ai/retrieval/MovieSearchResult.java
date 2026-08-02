package congtuong.dev.cinemabooking.ai.retrieval;

import java.util.UUID;

public record MovieSearchResult(
        UUID movieId,
        String title,
        String content,
        Double score
) {
}
