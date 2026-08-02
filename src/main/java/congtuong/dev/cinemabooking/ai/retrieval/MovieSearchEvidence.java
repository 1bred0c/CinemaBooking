package congtuong.dev.cinemabooking.ai.retrieval;

public record MovieSearchEvidence(
        SearchChannel channel,
        int rank,
        double rawScore
) {
}
