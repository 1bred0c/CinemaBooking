package congtuong.dev.cinemabooking.repository;

import congtuong.dev.cinemabooking.ai.query.MovieSearchPlan;
import congtuong.dev.cinemabooking.entity.enums.AgeRating;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class MovieKeywordSearchRepository {

    private final JdbcClient jdbcClient;

    public List<MovieKeywordHit> search(MovieSearchPlan plan, int limit) {
        String keyword = firstNonBlank(plan.keywordQuery(), plan.semanticQuery());
        List<String> genres = plan.genres() == null ? List.of() : plan.genres();
        List<String> allowedRatings = allowedRatings(plan.viewerAge()).stream()
                .map(Enum::name)
                .toList();

        String sql = """
                WITH movie_search AS (
                    SELECT m.id,
                           m.title,
                           m.description,
                           m.director,
                           m.duration_minutes,
                           m.release_date,
                           m.age_rating,
                           concat_ws(' ', m.title, m.description, m.director,
                               string_agg(g.name, ' ')) AS searchable
                    FROM movies m
                    LEFT JOIN movie_genre mg ON mg.movie_id = m.id
                    LEFT JOIN genres g ON g.id = mg.genre_id
                    WHERE m.is_active = true
                    GROUP BY m.id
                )
                SELECT id,
                       title,
                       (CASE WHEN CAST(:exactTitle AS text) IS NOT NULL
                                  AND lower(title) = lower(CAST(:exactTitle AS text))
                             THEN 10.0 ELSE 0.0 END
                        + CASE WHEN CAST(:exactTitle AS text) IS NOT NULL
                                   AND title ILIKE concat('%', CAST(:exactTitle AS text), '%')
                              THEN 4.0 ELSE 0.0 END
                        + CASE WHEN CAST(:director AS text) IS NOT NULL
                                   AND director ILIKE concat('%', CAST(:director AS text), '%')
                              THEN 3.0 ELSE 0.0 END
                        + CASE WHEN CAST(:filterGenres AS boolean) = true THEN
                              (SELECT count(DISTINCT lower(g3.name))
                               FROM movie_genre mg3
                               JOIN genres g3 ON g3.id = mg3.genre_id
                               WHERE mg3.movie_id = movie_search.id
                                 AND lower(g3.name) IN (:genres))
                              ELSE 0.0 END
                        + CASE WHEN CAST(:keyword AS text) IS NOT NULL THEN
                              ts_rank_cd(
                                  to_tsvector('simple', coalesce(searchable, '')),
                                  websearch_to_tsquery('simple', CAST(:keyword AS text))
                              )
                              ELSE 0.0 END) AS score
                FROM movie_search
                WHERE (CAST(:maximumDuration AS integer) IS NULL
                       OR duration_minutes <= CAST(:maximumDuration AS integer))
                  AND (CAST(:releasedAfter AS date) IS NULL
                       OR release_date >= CAST(:releasedAfter AS date))
                  AND (CAST(:filterRating AS boolean) = false
                       OR age_rating IN (:allowedRatings))
                  AND (CAST(:filterGenres AS boolean) = false OR id IN (
                      SELECT mg2.movie_id
                      FROM movie_genre mg2
                      JOIN genres g2 ON g2.id = mg2.genre_id
                      WHERE lower(g2.name) IN (:genres)
                      GROUP BY mg2.movie_id
                      HAVING count(DISTINCT lower(g2.name)) > 0
                  ))
                  AND (
                      (CAST(:keyword AS text) IS NOT NULL AND (
                          to_tsvector('simple', coalesce(searchable, ''))
                              @@ websearch_to_tsquery('simple', CAST(:keyword AS text))
                          OR searchable ILIKE concat('%', CAST(:keyword AS text), '%')
                      ))
                      OR (CAST(:exactTitle AS text) IS NOT NULL
                          AND title ILIKE concat('%', CAST(:exactTitle AS text), '%'))
                      OR (CAST(:director AS text) IS NOT NULL
                          AND director ILIKE concat('%', CAST(:director AS text), '%'))
                      OR (CAST(:filterGenres AS boolean) = true AND EXISTS (
                          SELECT 1
                          FROM movie_genre mg4
                          JOIN genres g4 ON g4.id = mg4.genre_id
                          WHERE mg4.movie_id = movie_search.id
                            AND lower(g4.name) IN (:genres)
                      ))
                  )
                ORDER BY score DESC, title
                LIMIT :limit
                """;

        return jdbcClient.sql(sql)
                .param("keyword", blankToNull(keyword))
                .param("exactTitle", blankToNull(plan.exactTitle()))
                .param("director", blankToNull(plan.director()))
                .param("maximumDuration", plan.maximumDurationMinutes())
                .param("releasedAfter", plan.releasedAfter())
                .param("filterRating", plan.viewerAge() != null)
                .param("allowedRatings", allowedRatings.isEmpty()
                        ? List.of("__NONE__") : allowedRatings)
                .param("filterGenres", !genres.isEmpty())
                .param("genres", genres.isEmpty()
                        ? List.of("__none__")
                        : genres.stream().map(String::toLowerCase).toList())
                .param("limit", limit)
                .query((row, rowNumber) -> new MovieKeywordHit(
                        row.getObject("id", java.util.UUID.class),
                        row.getString("title"),
                        row.getDouble("score")
                ))
                .list();
    }

    private List<AgeRating> allowedRatings(Integer viewerAge) {
        if (viewerAge == null) {
            return List.of();
        }
        List<AgeRating> ratings = new ArrayList<>(List.of(AgeRating.P));
        if (viewerAge >= 6) ratings.add(AgeRating.K);
        if (viewerAge >= 13) ratings.add(AgeRating.T13);
        if (viewerAge >= 16) ratings.add(AgeRating.T16);
        if (viewerAge >= 18) ratings.add(AgeRating.T18);
        return ratings;
    }

    private String firstNonBlank(String first, String second) {
        return blankToNull(first) != null ? first : second;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
