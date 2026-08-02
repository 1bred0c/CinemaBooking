package congtuong.dev.cinemabooking.ai.ranking;

public record MovieRerankItem(
        String movieId,
        Double relevanceScore,
        String reason
) {
}
