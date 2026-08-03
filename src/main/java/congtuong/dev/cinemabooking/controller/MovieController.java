package congtuong.dev.cinemabooking.controller;

import congtuong.dev.cinemabooking.dto.request.MovieCreateRequest;
import congtuong.dev.cinemabooking.dto.request.MovieUpdateRequest;
import congtuong.dev.cinemabooking.dto.request.MovieFilterRequest;
import congtuong.dev.cinemabooking.dto.response.MovieResponse;
import congtuong.dev.cinemabooking.dto.response.ShowtimeBrowseResponse;
import congtuong.dev.cinemabooking.dto.response.MoviePosterResponse;
import congtuong.dev.cinemabooking.service.MovieService;
import congtuong.dev.cinemabooking.service.ShowtimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;

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
    public Page<MovieResponse> getMovies(
            @ModelAttribute MovieFilterRequest filter,
            @PageableDefault(
                    size = 20,
                    sort = "title",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        return movieService.getMovies(filter, pageable);
    }

    @GetMapping("/now-showing")
    public Page<MovieResponse> getNowShowingMovies(
            @PageableDefault(
                    size = 20,
                    sort = "releaseDate",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return movieService.getNowShowingMovies(pageable);
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

    @PostMapping(
            value = "/{movieId}/poster",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<MoviePosterResponse> uploadPoster(
            @PathVariable UUID movieId,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(movieService.uploadPoster(movieId, file));
    }

    @DeleteMapping("/{movieId}/poster")
    public ResponseEntity<Void> deletePoster(
            @PathVariable UUID movieId
    ) {
        movieService.deletePoster(movieId);
        return ResponseEntity.noContent().build();
    }
}
