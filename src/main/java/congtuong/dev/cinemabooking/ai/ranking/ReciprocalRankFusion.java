package congtuong.dev.cinemabooking.ai.ranking;

import congtuong.dev.cinemabooking.ai.retrieval.MovieSearchEvidence;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReciprocalRankFusion {

    public double score(List<MovieSearchEvidence> evidence, int constant) {
        return evidence.stream()
                .mapToDouble(item -> 1.0 / (constant + item.rank()))
                .sum();
    }
}
