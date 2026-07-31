package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.MovieCreateRequest;
import congtuong.dev.cinemabooking.dto.request.MovieUpdateRequest;
import congtuong.dev.cinemabooking.dto.response.GenreSummaryResponse;
import congtuong.dev.cinemabooking.dto.response.MovieResponse;
import congtuong.dev.cinemabooking.entity.Genre;
import congtuong.dev.cinemabooking.entity.Movie;
import congtuong.dev.cinemabooking.entity.enums.ShowtimeStatus;
import congtuong.dev.cinemabooking.exception.GenreException;
import congtuong.dev.cinemabooking.exception.MovieException;
import congtuong.dev.cinemabooking.repository.GenreRepository;
import congtuong.dev.cinemabooking.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieServiceImpl implements MovieService {
    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;

    @Override
    public List<MovieResponse> getMovies() {
        return movieRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public List<MovieResponse> getNowShowingMovies() {
        return movieRepository.findNowShowing(
                        ShowtimeStatus.SCHEDULED,
                        LocalDateTime.now()
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public MovieResponse getMovie(UUID id) {
        return toResponse(findMovie(id));
    }

    @Override
    @Transactional
    public MovieResponse createMovie(MovieCreateRequest request) {
        Movie movie = Movie.builder()
                .title(request.title())
                .description(request.description())
                .durationMinutes(request.durationMinutes())
                .releaseDate(request.releaseDate())
                .director(request.director())
                .posterUrl(request.posterUrl())
                .trailerUrl(request.trailerUrl())
                .ageRating(request.ageRating())
                .build();
        return toResponse(movieRepository.save(movie));
    }

    @Override
    @Transactional
    public MovieResponse updateMovie(UUID id, MovieUpdateRequest request) {
        Movie movie = findMovie(id);
        if (request.title() != null) movie.setTitle(request.title());
        if (request.description() != null) movie.setDescription(request.description());
        if (request.durationMinutes() != null) movie.setDurationMinutes(request.durationMinutes());
        if (request.releaseDate() != null) movie.setReleaseDate(request.releaseDate());
        if (request.director() != null) movie.setDirector(request.director());
        if (request.posterUrl() != null) movie.setPosterUrl(request.posterUrl());
        if (request.trailerUrl() != null) movie.setTrailerUrl(request.trailerUrl());
        if (request.ageRating() != null) movie.setAgeRating(request.ageRating());
        return toResponse(movie);
    }

    @Override
    @Transactional
    public void deactivateMovie(UUID id) {
        findMovie(id).setActive(false);
    }

    @Override
    @Transactional
    public MovieResponse addGenre(UUID movieId, UUID genreId) {
        Movie movie = findMovie(movieId);
        Genre genre = findGenre(genreId);
        movie.getGenres().add(genre);
        return toResponse(movie);
    }

    @Override
    @Transactional
    public MovieResponse removeGenre(UUID movieId, UUID genreId) {
        Movie movie = findMovie(movieId);
        Genre genre = findGenre(genreId);
        movie.getGenres().remove(genre);
        return toResponse(movie);
    }

    private Movie findMovie(UUID id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new MovieException("Movie not found"));
    }

    private Genre findGenre(UUID id) {
        return genreRepository.findById(id)
                .orElseThrow(() -> new GenreException("Genre not found"));
    }

    private MovieResponse toResponse(Movie movie) {
        List<GenreSummaryResponse> genres = movie.getGenres().stream()
                .map(this::toGenreSummaryResponse)
                .toList();
        return new MovieResponse(movie.getId(), movie.getTitle(), movie.getDescription(),
                movie.getDurationMinutes(), movie.getReleaseDate(), movie.getDirector(),
                movie.getPosterUrl(), movie.getTrailerUrl(), movie.getAgeRating(),
                movie.isActive(), genres, movie.getCreatedAt(), movie.getUpdatedAt());
    }

    private GenreSummaryResponse toGenreSummaryResponse(Genre genre) {
        return new GenreSummaryResponse(genre.getId(), genre.getName());
    }
}
