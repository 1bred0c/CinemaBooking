package congtuong.dev.cinemabooking.ai.query;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatQueryAnalyzerImpl implements ChatQueryAnalyzer {

    private static final String SYSTEM_PROMPT = """
            You analyze messages for a CinemaBooking movie assistant.
            Return a structured query plan and never answer the user.

            Intent rules:
            - MOVIE_SEARCH: recommendations or discovery.
            - MOVIE_INFORMATION: facts about a named movie.
            - GREETING: greeting or thanks only.
            - HELP: asks what the assistant can do.
            - LIVE_DATA: current showtimes, cinemas, seats, prices, bookings.
            - OUT_OF_SCOPE: unrelated to movies or cinemas.

            Extraction rules:
            - semanticQuery keeps themes, mood, setting and story intent.
            - keywordQuery keeps important literal words and named entities.
            - exactTitle and director are only set when explicitly mentioned.
            - genres contains conventional movie genres explicitly stated or
              strongly implied by the request. Use these canonical English
              names when applicable: Science Fiction, Adventure, Drama,
              Survival, Horror, Thriller, Animation, Family, Action, Romance,
              Musical, Fantasy, War, Comedy, Social Commentary.
            - convert hours to maximumDurationMinutes.
            - viewerAge is the viewer's age, not an age-rating enum.
            - do not invent constraints. Use null or an empty list when absent.
            - preserve the user's language in search query text.
            """;

    private final ChatClient chatClient;

    private static final Map<String, String> GENRE_ALIASES = Map.ofEntries(
            Map.entry("khoa học viễn tưởng", "Science Fiction"),
            Map.entry("viễn tưởng", "Science Fiction"),
            Map.entry("phiêu lưu", "Adventure"),
            Map.entry("chính kịch", "Drama"),
            Map.entry("sinh tồn", "Survival"),
            Map.entry("kinh dị", "Horror"),
            Map.entry("giật gân", "Thriller"),
            Map.entry("hoạt hình", "Animation"),
            Map.entry("gia đình", "Family"),
            Map.entry("hành động", "Action"),
            Map.entry("tình cảm", "Romance"),
            Map.entry("lãng mạn", "Romance"),
            Map.entry("ca nhạc", "Musical"),
            Map.entry("xã hội", "Social Commentary")
    );

    @Override
    public ChatQueryPlan analyze(String message) {
        try {
            ChatQueryPlan result = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(message)
                    .call()
                    .entity(ChatQueryPlan.class, spec -> spec.validateSchema());
            return normalize(result, message);
        } catch (RuntimeException exception) {
            log.warn(
                    "Movie query analysis failed; using semantic fallback: {}",
                    exception.getMessage()
            );
            return fallback(message);
        }
    }

    private ChatQueryPlan normalize(ChatQueryPlan plan, String message) {
        if (plan == null || plan.intent() == null) {
            return fallback(message);
        }
        MovieSearchPlan search = plan.movieSearch();
        if (plan.requiresMovieSearch() && search == null) {
            search = fallbackSearch(message);
        } else if (search != null) {
            search = new MovieSearchPlan(
                    textOrDefault(search.semanticQuery(), message),
                    trimToNull(search.keywordQuery()),
                    trimToNull(search.exactTitle()),
                    trimToNull(search.director()),
                    search.genres() == null
                            ? List.of()
                            : search.genres().stream()
                                    .filter(value -> value != null && !value.isBlank())
                                    .map(String::trim)
                                    .map(this::normalizeGenre)
                                    .distinct()
                                    .toList(),
                    positiveOrNull(search.maximumDurationMinutes()),
                    validAgeOrNull(search.viewerAge()),
                    search.releasedAfter()
            );
        }
        double confidence = plan.confidence() == null
                ? 0.5
                : Math.max(0, Math.min(1, plan.confidence()));
        return new ChatQueryPlan(plan.intent(), confidence, search);
    }

    private ChatQueryPlan fallback(String message) {
        return new ChatQueryPlan(
                ChatIntent.MOVIE_SEARCH,
                0.0,
                fallbackSearch(message)
        );
    }

    private MovieSearchPlan fallbackSearch(String message) {
        return new MovieSearchPlan(
                message,
                message,
                null,
                null,
                List.of(),
                null,
                null,
                null
        );
    }

    private String textOrDefault(String value, String fallback) {
        String normalized = trimToNull(value);
        return normalized == null ? fallback : normalized;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Integer positiveOrNull(Integer value) {
        return value != null && value > 0 ? value : null;
    }

    Integer validAgeOrNull(Integer value) {
        // Structured output providers may deserialize an omitted numeric field as 0.
        // A real viewer age must be explicitly positive; otherwise no age filter applies.
        return value != null && value > 0 && value <= 120 ? value : null;
    }

    private String normalizeGenre(String value) {
        return GENRE_ALIASES.getOrDefault(
                value.toLowerCase(Locale.ROOT),
                value
        );
    }
}
