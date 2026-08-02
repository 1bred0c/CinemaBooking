package congtuong.dev.cinemabooking.ai.ranking;

import congtuong.dev.cinemabooking.ai.retrieval.MovieSearchEvidence;
import congtuong.dev.cinemabooking.ai.retrieval.SearchChannel;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReciprocalRankFusionTest {

    @Test
    void candidateSupportedByTwoChannelsBeatsSingleChannelCandidate() {
        ReciprocalRankFusion fusion = new ReciprocalRankFusion();
        double hybrid = fusion.score(List.of(
                new MovieSearchEvidence(SearchChannel.VECTOR, 5, 0.38),
                new MovieSearchEvidence(SearchChannel.KEYWORD, 1, 2.0)
        ), 60);
        double vectorOnly = fusion.score(List.of(
                new MovieSearchEvidence(SearchChannel.VECTOR, 1, 0.44)
        ), 60);

        assertTrue(hybrid > vectorOnly);
    }
}
