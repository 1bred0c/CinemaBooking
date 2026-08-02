package congtuong.dev.cinemabooking.ai.query;

import java.time.LocalDate;
import java.util.List;

public record MovieSearchPlan(
        String semanticQuery,
        String keywordQuery,
        String exactTitle,
        String director,
        List<String> genres,
        Integer maximumDurationMinutes,
        Integer viewerAge,
        LocalDate releasedAfter
) {
}
