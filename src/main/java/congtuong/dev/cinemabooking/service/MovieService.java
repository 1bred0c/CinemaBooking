package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.MovieCreateRequest;
import congtuong.dev.cinemabooking.dto.request.MovieUpdateRequest;
import congtuong.dev.cinemabooking.dto.request.MovieFilterRequest;
import congtuong.dev.cinemabooking.dto.response.MovieResponse;
import congtuong.dev.cinemabooking.dto.response.MoviePosterResponse;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface MovieService {
    Page<MovieResponse> getMovies(MovieFilterRequest filter, Pageable pageable);
    Page<MovieResponse> getNowShowingMovies(Pageable pageable);
    MovieResponse getMovie(UUID id);
    MovieResponse createMovie(MovieCreateRequest request);
    MovieResponse updateMovie(UUID id, MovieUpdateRequest request);
    void deactivateMovie(UUID id);
    MovieResponse addGenre(UUID movieId, UUID genreId);
    MovieResponse removeGenre(UUID movieId, UUID genreId);
    MoviePosterResponse uploadPoster(UUID movieId, MultipartFile file);
    void deletePoster(UUID movieId);
}
