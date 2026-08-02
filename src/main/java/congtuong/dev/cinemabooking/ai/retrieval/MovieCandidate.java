package congtuong.dev.cinemabooking.ai.retrieval;

import java.util.List;
import java.util.UUID;

public record MovieCandidate(
        UUID movieId,
        String title,
        String content,
        double fusionScore,
        int genreMatchCount,
        List<MovieSearchEvidence> evidence
) {
}
