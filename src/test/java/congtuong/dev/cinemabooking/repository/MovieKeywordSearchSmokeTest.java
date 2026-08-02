package congtuong.dev.cinemabooking.repository;

import congtuong.dev.cinemabooking.ai.query.MovieSearchPlan;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

@SpringBootTest(properties = {
        "spring.kafka.listener.auto-startup=false",
        "booking.expiration-enabled=false"
})
@EnabledIfEnvironmentVariable(
        named = "RUN_AI_HYBRID_LIVE_TEST",
        matches = "true"
)
class MovieKeywordSearchSmokeTest {

    @Autowired
    private MovieKeywordSearchRepository repository;

    @Test
    void executesHybridKeywordSqlAgainstConfiguredPostgres() {
        var hits = repository.search(new MovieSearchPlan(
                "The Martian",
                "The Martian",
                "The Martian",
                null,
                List.of(),
                150,
                13,
                null
        ), 10);

        assertFalse(hits.isEmpty());
    }

    @Test
    void findsCandidatesByGenreWhenTextDoesNotMatch() {
        var hits = repository.search(new MovieSearchPlan(
                "__no_text_match__",
                "__no_text_match__",
                null,
                null,
                List.of("Action"),
                null,
                null,
                null
        ), 10);

        assertFalse(hits.isEmpty());
    }
}
