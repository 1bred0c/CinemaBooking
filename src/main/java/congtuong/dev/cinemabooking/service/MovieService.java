package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.MovieCreateRequest;
import congtuong.dev.cinemabooking.dto.request.MovieUpdateRequest;
import congtuong.dev.cinemabooking.dto.response.MovieResponse;

import java.util.List;
import java.util.UUID;

public interface MovieService {
    List<MovieResponse> getMovies();
    List<MovieResponse> getNowShowingMovies();
    MovieResponse getMovie(UUID id);
    MovieResponse createMovie(MovieCreateRequest request);
    MovieResponse updateMovie(UUID id, MovieUpdateRequest request);
    void deactivateMovie(UUID id);
    MovieResponse addGenre(UUID movieId, UUID genreId);
    MovieResponse removeGenre(UUID movieId, UUID genreId);
}
