package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.ai.indexing.MovieIndexAction;
import congtuong.dev.cinemabooking.ai.indexing.MovieIndexRequested;
import congtuong.dev.cinemabooking.dto.request.MovieCreateRequest;
import congtuong.dev.cinemabooking.dto.request.MovieUpdateRequest;
import congtuong.dev.cinemabooking.dto.response.GenreSummaryResponse;
import congtuong.dev.cinemabooking.dto.response.MovieResponse;
import congtuong.dev.cinemabooking.dto.response.MoviePosterResponse;
import congtuong.dev.cinemabooking.entity.Genre;
import congtuong.dev.cinemabooking.entity.Movie;
import congtuong.dev.cinemabooking.entity.enums.ShowtimeStatus;
import congtuong.dev.cinemabooking.exception.GenreException;
import congtuong.dev.cinemabooking.exception.MovieException;
import congtuong.dev.cinemabooking.exception.MediaException;
import congtuong.dev.cinemabooking.media.MediaStorageService;
import congtuong.dev.cinemabooking.media.StoredMedia;
import congtuong.dev.cinemabooking.repository.GenreRepository;
import congtuong.dev.cinemabooking.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieServiceImpl implements MovieService {
    private static final long MAX_POSTER_SIZE = 5L * 1024L * 1024L;
    private static final String POSTER_FOLDER = "cinema-booking/movies/";

    private final MovieRepository movieRepository;
    private final GenreRepository genreRepository;
    private final MediaStorageService mediaStorageService;
    private final ApplicationEventPublisher eventPublisher;

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
        Movie savedMovie = movieRepository.save(movie);
        requestReindex(savedMovie.getId());
        return toResponse(savedMovie);
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
        requestReindex(movie.getId());
        return toResponse(movie);
    }

    @Override
    @Transactional
    public void deactivateMovie(UUID id) {
        findMovie(id).setActive(false);
        eventPublisher.publishEvent(new MovieIndexRequested(
                id,
                MovieIndexAction.REMOVE
        ));
    }

    @Override
    @Transactional
    public MovieResponse addGenre(UUID movieId, UUID genreId) {
        Movie movie = findMovie(movieId);
        Genre genre = findGenre(genreId);
        movie.getGenres().add(genre);
        requestReindex(movie.getId());
        return toResponse(movie);
    }

    @Override
    @Transactional
    public MovieResponse removeGenre(UUID movieId, UUID genreId) {
        Movie movie = findMovie(movieId);
        Genre genre = findGenre(genreId);
        movie.getGenres().remove(genre);
        requestReindex(movie.getId());
        return toResponse(movie);
    }

    @Override
    @Transactional
    public MoviePosterResponse uploadPoster(
            UUID movieId,
            MultipartFile file
    ) {
        Movie movie = findMovie(movieId);
        byte[] content = validateAndReadPoster(file);
        String publicId = posterPublicId(movieId);
        StoredMedia storedMedia = mediaStorageService.uploadImage(
                content,
                publicId
        );
        movie.setPosterUrl(storedMedia.secureUrl());
        return new MoviePosterResponse(
                movieId,
                storedMedia.secureUrl(),
                storedMedia.publicId()
        );
    }

    @Override
    @Transactional
    public void deletePoster(UUID movieId) {
        Movie movie = findMovie(movieId);
        mediaStorageService.deleteImage(posterPublicId(movieId));
        movie.setPosterUrl(null);
    }

    private Movie findMovie(UUID id) {
        return movieRepository.findById(id)
                .orElseThrow(() -> new MovieException("Movie not found"));
    }

    private Genre findGenre(UUID id) {
        return genreRepository.findById(id)
                .orElseThrow(() -> new GenreException("Genre not found"));
    }

    private byte[] validateAndReadPoster(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalidPoster("Poster file is required");
        }
        if (file.getSize() > MAX_POSTER_SIZE) {
            throw invalidPoster("Poster file must not exceed 5 MB");
        }
        try {
            byte[] content = file.getBytes();
            if (!isSupportedImage(content)) {
                throw invalidPoster(
                        "Poster must be a JPEG, PNG, or WebP image"
                );
            }
            return content;
        } catch (IOException exception) {
            throw invalidPoster("Unable to read poster file");
        }
    }

    private boolean isSupportedImage(byte[] content) {
        return isJpeg(content) || isPng(content) || isWebp(content);
    }

    private boolean isJpeg(byte[] content) {
        return content.length >= 3
                && unsigned(content[0]) == 0xFF
                && unsigned(content[1]) == 0xD8
                && unsigned(content[2]) == 0xFF;
    }

    private boolean isPng(byte[] content) {
        int[] signature = {
                0x89, 0x50, 0x4E, 0x47,
                0x0D, 0x0A, 0x1A, 0x0A
        };
        if (content.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if (unsigned(content[index]) != signature[index]) {
                return false;
            }
        }
        return true;
    }

    private boolean isWebp(byte[] content) {
        return content.length >= 12
                && content[0] == 'R'
                && content[1] == 'I'
                && content[2] == 'F'
                && content[3] == 'F'
                && content[8] == 'W'
                && content[9] == 'E'
                && content[10] == 'B'
                && content[11] == 'P';
    }

    private int unsigned(byte value) {
        return Byte.toUnsignedInt(value);
    }

    private String posterPublicId(UUID movieId) {
        return POSTER_FOLDER + movieId + "/poster";
    }

    private MediaException invalidPoster(String message) {
        return new MediaException(HttpStatus.BAD_REQUEST, message);
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

    private void requestReindex(UUID movieId) {
        eventPublisher.publishEvent(new MovieIndexRequested(
                movieId,
                MovieIndexAction.REINDEX
        ));
    }
}
