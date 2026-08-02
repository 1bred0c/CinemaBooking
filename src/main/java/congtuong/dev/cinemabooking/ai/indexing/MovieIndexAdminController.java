package congtuong.dev.cinemabooking.ai.indexing;

import congtuong.dev.cinemabooking.ai.indexing.dto.MovieIndexResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/ai/index/movies")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class MovieIndexAdminController {

    private final MovieIndexingService movieIndexingService;

    @PostMapping("/{movieId}")
    public MovieIndexResponse reindexMovie(@PathVariable UUID movieId) {
        movieIndexingService.reindexMovie(movieId);
        return new MovieIndexResponse(1, "Movie indexed successfully");
    }

    @PostMapping("/reindex-all")
    public MovieIndexResponse reindexAllMovies() {
        int indexedMovies = movieIndexingService.reindexAllMovies();
        return new MovieIndexResponse(
                indexedMovies,
                "Active movies indexed successfully"
        );
    }
}
