package congtuong.dev.cinemabooking.ai.ranking;

import congtuong.dev.cinemabooking.ai.query.MovieSearchPlan;
import congtuong.dev.cinemabooking.ai.rag.config.MovieRagProperties;
import congtuong.dev.cinemabooking.ai.retrieval.MovieCandidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MovieRerankerImpl implements MovieReranker {

    private static final String SYSTEM_PROMPT = """
            You rerank movie candidates for CinemaBooking.
            Judge relevance to the original user request and extracted search
            constraints. Return only IDs from the candidate list. Never invent
            a movie. Exclude clearly irrelevant candidates. Give a score from
            0 to 1 and a short reason in the user's language.
            """;

    private final ChatClient chatClient;
    private final MovieRagProperties properties;

    @Override
    public List<RankedMovie> rerank(
            String userMessage,
            MovieSearchPlan plan,
            List<MovieCandidate> candidates
    ) {
        if (candidates.isEmpty()) {
            return List.of();
        }
        if (!properties.rerankEnabled()) {
            log.debug("Movie reranking is disabled; using hybrid ranking");
            return fallback(candidates);
        }
        try {
            String candidateText = candidates.stream()
                    .map(candidate -> """
                            [Movie ID: %s]
                            Fusion score: %s
                            %s
                            """.formatted(
                            candidate.movieId(),
                            candidate.fusionScore(),
                            candidate.content()
                    ).strip())
                    .collect(Collectors.joining("\n\n---\n\n"));

            MovieRerankDecision decision = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user("""
                            Original request:
                            %s

                            Extracted search plan:
                            %s

                            Candidates:
                            %s
                            """.formatted(userMessage, plan, candidateText))
                    .call()
                    .entity(
                            MovieRerankDecision.class,
                            spec -> spec.validateSchema()
                    );
            List<RankedMovie> validated = validate(decision, candidates);
            if (validated.isEmpty()) {
                log.debug(
                        "Movie reranker returned no valid candidate; using hybrid ranking"
                );
                return fallback(candidates);
            }
            logRankedMovies(validated);
            return validated;
        } catch (RuntimeException exception) {
            log.warn(
                    "Movie reranking failed; using RRF fallback: {}",
                    exception.getMessage()
            );
            return fallback(candidates);
        }
    }

    private void logRankedMovies(List<RankedMovie> movies) {
        if (!log.isDebugEnabled()) return;
        for (int index = 0; index < movies.size(); index++) {
            RankedMovie movie = movies.get(index);
            log.debug(
                    "LLM reranked candidate: rank={}, movieId={}, title='{}', "
                            + "relevanceScore={}, reason='{}'",
                    index + 1,
                    movie.movieId(),
                    movie.title(),
                    movie.relevanceScore(),
                    movie.reason()
            );
        }
    }

    private List<RankedMovie> validate(
            MovieRerankDecision decision,
            List<MovieCandidate> candidates
    ) {
        if (decision == null || decision.rankings() == null) {
            return List.of();
        }
        Map<UUID, MovieCandidate> allowed = candidates.stream()
                .collect(Collectors.toMap(
                        MovieCandidate::movieId,
                        Function.identity()
                ));
        return decision.rankings().stream()
                .filter(item -> item != null && item.movieId() != null)
                .map(item -> toRanked(item, allowed))
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toMap(
                        RankedMovie::movieId,
                        Function.identity(),
                        (first, ignored) -> first
                ))
                .values().stream()
                .sorted(Comparator.comparingDouble(
                        RankedMovie::relevanceScore
                ).reversed())
                .limit(properties.topK())
                .toList();
    }

    private RankedMovie toRanked(
            MovieRerankItem item,
            Map<UUID, MovieCandidate> allowed
    ) {
        try {
            UUID id = UUID.fromString(item.movieId());
            MovieCandidate candidate = allowed.get(id);
            if (candidate == null) return null;
            double score = item.relevanceScore() == null
                    ? 0
                    : Math.max(0, Math.min(1, item.relevanceScore()));
            return new RankedMovie(
                    id,
                    candidate.title(),
                    candidate.content(),
                    score,
                    item.reason()
            );
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private List<RankedMovie> fallback(List<MovieCandidate> candidates) {
        double max = candidates.stream()
                .mapToDouble(MovieCandidate::fusionScore)
                .max()
                .orElse(1);
        return candidates.stream()
                .limit(properties.topK())
                .map(candidate -> new RankedMovie(
                        candidate.movieId(),
                        candidate.title(),
                        candidate.content(),
                        max == 0 ? 0 : candidate.fusionScore() / max,
                        "Ranked by hybrid retrieval"
                ))
                .toList();
    }
}
