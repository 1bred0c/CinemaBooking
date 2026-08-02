package congtuong.dev.cinemabooking.ai.retrieval;

import congtuong.dev.cinemabooking.ai.indexing.MovieDocumentFactory;
import congtuong.dev.cinemabooking.ai.query.MovieSearchPlan;
import congtuong.dev.cinemabooking.ai.rag.config.MovieRagProperties;
import congtuong.dev.cinemabooking.ai.ranking.ReciprocalRankFusion;
import congtuong.dev.cinemabooking.entity.Genre;
import congtuong.dev.cinemabooking.entity.Movie;
import congtuong.dev.cinemabooking.entity.enums.AgeRating;
import congtuong.dev.cinemabooking.repository.MovieKeywordHit;
import congtuong.dev.cinemabooking.repository.MovieKeywordSearchRepository;
import congtuong.dev.cinemabooking.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class HybridMovieRetrieverImpl implements HybridMovieRetriever {

    private final MovieKnowledgeRetriever vectorRetriever;
    private final MovieKeywordSearchRepository keywordRepository;
    private final MovieRepository movieRepository;
    private final MovieDocumentFactory documentFactory;
    private final ReciprocalRankFusion fusion;
    private final MovieRagProperties properties;

    @Override
    public List<MovieCandidate> search(MovieSearchPlan plan) {
        List<MovieSearchResult> vectorHits = vectorRetriever.search(
                plan.semanticQuery(),
                properties.candidateK(),
                properties.similarityThreshold()
        );
        List<MovieKeywordHit> keywordHits = keywordRepository.search(
                plan,
                properties.candidateK()
        );

        logSearchInputs(plan);
        logVectorHits(vectorHits);
        logKeywordHits(keywordHits);

        Map<UUID, List<MovieSearchEvidence>> evidence = new LinkedHashMap<>();
        for (int index = 0; index < vectorHits.size(); index++) {
            MovieSearchResult hit = vectorHits.get(index);
            evidence.computeIfAbsent(hit.movieId(), ignored -> new ArrayList<>())
                    .add(new MovieSearchEvidence(
                            SearchChannel.VECTOR,
                            index + 1,
                            hit.score() == null ? 0 : hit.score()
                    ));
        }
        for (int index = 0; index < keywordHits.size(); index++) {
            MovieKeywordHit hit = keywordHits.get(index);
            evidence.computeIfAbsent(hit.movieId(), ignored -> new ArrayList<>())
                    .add(new MovieSearchEvidence(
                            SearchChannel.KEYWORD,
                            index + 1,
                            hit.score()
                    ));
        }
        if (evidence.isEmpty()) {
            return List.of();
        }

        List<Movie> activeMovies = movieRepository
                .findAllByIdInAndActiveTrue(evidence.keySet())
                .stream()
                .toList();
        Map<UUID, Movie> movies = activeMovies.stream()
                .filter(movie -> matchesFilters(movie, plan, true))
                .collect(Collectors.toMap(Movie::getId, Function.identity()));

        if (movies.isEmpty() && plan.genres() != null && !plan.genres().isEmpty()) {
            log.debug(
                    "No candidate matched strict genres {}; retrying candidates "
                            + "without genre as a hard filter",
                    plan.genres()
            );
            movies = activeMovies.stream()
                    .filter(movie -> matchesFilters(movie, plan, false))
                    .collect(Collectors.toMap(Movie::getId, Function.identity()));
        }

        Map<UUID, Movie> eligibleMovies = movies;
        List<MovieCandidate> candidates = evidence.entrySet().stream()
                .filter(entry -> eligibleMovies.containsKey(entry.getKey()))
                .map(entry -> candidate(
                        eligibleMovies.get(entry.getKey()),
                        entry.getValue(),
                        plan
                ))
                .sorted(Comparator
                        .comparingInt(MovieCandidate::genreMatchCount)
                        .reversed()
                        .thenComparing(Comparator.comparingDouble(
                                MovieCandidate::fusionScore
                        ).reversed()))
                .limit(properties.candidateK())
                .toList();
        logFusedCandidates(candidates, evidence.size());
        return candidates;
    }

    private void logSearchInputs(MovieSearchPlan plan) {
        if (!log.isDebugEnabled()) return;
        log.debug(
                "Hybrid movie search plan: semanticQuery='{}', keywordQuery='{}', "
                        + "exactTitle='{}', director='{}', genres={}, maxDuration={}, "
                        + "viewerAge={}, releasedAfter={}",
                plan.semanticQuery(),
                plan.keywordQuery(),
                plan.exactTitle(),
                plan.director(),
                plan.genres(),
                plan.maximumDurationMinutes(),
                plan.viewerAge(),
                plan.releasedAfter()
        );
    }

    private void logVectorHits(List<MovieSearchResult> hits) {
        if (!log.isDebugEnabled()) return;
        for (int index = 0; index < hits.size(); index++) {
            MovieSearchResult hit = hits.get(index);
            log.debug(
                    "Vector candidate: rank={}, movieId={}, title='{}', score={}",
                    index + 1, hit.movieId(), hit.title(), hit.score()
            );
        }
    }

    private void logKeywordHits(List<MovieKeywordHit> hits) {
        if (!log.isDebugEnabled()) return;
        for (int index = 0; index < hits.size(); index++) {
            MovieKeywordHit hit = hits.get(index);
            log.debug(
                    "Keyword candidate: rank={}, movieId={}, title='{}', score={}",
                    index + 1, hit.movieId(), hit.title(), hit.score()
            );
        }
    }

    private void logFusedCandidates(List<MovieCandidate> candidates, int evidenceCount) {
        if (!log.isDebugEnabled()) return;
        log.debug(
                "Hybrid candidates after active/metadata filters: evidenceCount={}, candidateCount={}",
                evidenceCount, candidates.size()
        );
        for (int index = 0; index < candidates.size(); index++) {
            MovieCandidate candidate = candidates.get(index);
            log.debug(
                    "RRF candidate: rank={}, movieId={}, title='{}', fusionScore={}, "
                            + "genreMatchCount={}, evidence={}",
                    index + 1,
                    candidate.movieId(),
                    candidate.title(),
                    candidate.fusionScore(),
                    candidate.genreMatchCount(),
                    candidate.evidence()
            );
        }
    }

    private MovieCandidate candidate(
            Movie movie,
            List<MovieSearchEvidence> evidence,
            MovieSearchPlan plan
    ) {
        return new MovieCandidate(
                movie.getId(),
                movie.getTitle(),
                documentFactory.create(movie).getText(),
                fusion.score(evidence, properties.rrfConstant()),
                genreMatchCount(movie, plan),
                List.copyOf(evidence)
        );
    }

    private int genreMatchCount(Movie movie, MovieSearchPlan plan) {
        if (plan.genres() == null || plan.genres().isEmpty()) {
            return 0;
        }
        Set<String> requestedGenres = plan.genres().stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        return (int) movie.getGenres().stream()
                .map(Genre::getName)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .filter(requestedGenres::contains)
                .distinct()
                .count();
    }

    private boolean matchesFilters(
            Movie movie,
            MovieSearchPlan plan,
            boolean filterGenres
    ) {
        if (plan.maximumDurationMinutes() != null
                && movie.getDurationMinutes() > plan.maximumDurationMinutes()) {
            return false;
        }
        if (plan.releasedAfter() != null
                && (movie.getReleaseDate() == null
                || movie.getReleaseDate().isBefore(plan.releasedAfter()))) {
            return false;
        }
        if (plan.viewerAge() != null && !allowedFor(movie.getAgeRating(), plan.viewerAge())) {
            return false;
        }
        if (plan.director() != null && (movie.getDirector() == null
                || !movie.getDirector().toLowerCase(Locale.ROOT)
                .contains(plan.director().toLowerCase(Locale.ROOT)))) {
            return false;
        }
        if (plan.exactTitle() != null && !movie.getTitle()
                .toLowerCase(Locale.ROOT)
                .contains(plan.exactTitle().toLowerCase(Locale.ROOT))) {
            return false;
        }
        if (!filterGenres || plan.genres() == null || plan.genres().isEmpty()) {
            return true;
        }
        Set<String> movieGenres = movie.getGenres().stream()
                .map(Genre::getName)
                .map(name -> name.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        return plan.genres().stream()
                .map(name -> name.toLowerCase(Locale.ROOT))
                .anyMatch(movieGenres::contains);
    }

    private boolean allowedFor(AgeRating rating, int viewerAge) {
        if (rating == null || rating == AgeRating.P) return true;
        return switch (rating) {
            case K -> viewerAge >= 6;
            case T13 -> viewerAge >= 13;
            case T16 -> viewerAge >= 16;
            case T18 -> viewerAge >= 18;
            case C -> false;
            case P -> true;
        };
    }
}
