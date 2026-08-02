package congtuong.dev.cinemabooking.ai.ranking;

import java.util.UUID;

public record RankedMovie(
        UUID movieId,
        String title,
        String content,
        double relevanceScore,
        String reason
) {
}
