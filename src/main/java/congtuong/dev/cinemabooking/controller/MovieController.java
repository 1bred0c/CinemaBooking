package congtuong.dev.cinemabooking.controller;

import congtuong.dev.cinemabooking.dto.request.MovieCreateRequest;
import congtuong.dev.cinemabooking.dto.request.MovieUpdateRequest;
import congtuong.dev.cinemabooking.dto.response.MovieResponse;
import congtuong.dev.cinemabooking.dto.response.ShowtimeBrowseResponse;
import congtuong.dev.cinemabooking.service.MovieService;
import congtuong.dev.cinemabooking.service.ShowtimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/movies")
@RequiredArgsConstructor
public class MovieController {
    private final MovieService movieService;
    private final ShowtimeService showtimeService;

    @GetMapping
    public List<MovieResponse> getMovies() {
        return movieService.getMovies();
    }

    @GetMapping("/now-showing")
    public List<MovieResponse> getNowShowingMovies() {
        return movieService.getNowShowingMovies();
    }

    @GetMapping("/{id}")
    public MovieResponse getMovie(@PathVariable UUID id) {
        return movieService.getMovie(id);
    }

    @GetMapping("/{movieId}/showtimes")
    public List<ShowtimeBrowseResponse> getBookableShowtimes(
            @PathVariable UUID movieId,
            @RequestParam(required = false) UUID cinemaId,
            @RequestParam(required = false) LocalDate date
    ) {
        return showtimeService.getBookableShowtimesByMovie(
                movieId,
                cinemaId,
                date
        );
    }

    @PostMapping
    public ResponseEntity<MovieResponse> createMovie(@Valid @RequestBody MovieCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(movieService.createMovie(request));
    }

    @PatchMapping("/{id}")
    public MovieResponse updateMovie(@PathVariable UUID id,
                                     @Valid @RequestBody MovieUpdateRequest request) {
        return movieService.updateMovie(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateMovie(@PathVariable UUID id) {
        movieService.deactivateMovie(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{movieId}/genres/{genreId}")
    public MovieResponse addGenre(@PathVariable UUID movieId, @PathVariable UUID genreId) {
        return movieService.addGenre(movieId, genreId);
    }

    @DeleteMapping("/{movieId}/genres/{genreId}")
    public MovieResponse removeGenre(@PathVariable UUID movieId, @PathVariable UUID genreId) {
        return movieService.removeGenre(movieId, genreId);
    }
}
