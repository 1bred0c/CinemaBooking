package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.MovieCreateRequest;
import congtuong.dev.cinemabooking.dto.request.MovieUpdateRequest;
import congtuong.dev.cinemabooking.dto.response.MovieResponse;
import congtuong.dev.cinemabooking.entity.Genre;
import congtuong.dev.cinemabooking.entity.Movie;
import congtuong.dev.cinemabooking.entity.enums.AgeRating;
import congtuong.dev.cinemabooking.exception.MovieException;
import congtuong.dev.cinemabooking.repository.GenreRepository;
import congtuong.dev.cinemabooking.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MovieServiceImplTest {
    @Mock
    private MovieRepository movieRepository;
    @Mock
    private GenreRepository genreRepository;
    @InjectMocks
    private MovieServiceImpl movieService;

    @Test
    void createMovieMapsAndSavesMovie() {
        MovieCreateRequest request = new MovieCreateRequest("Dune", "Science fiction", 155,
                LocalDate.of(2021, 10, 22), "Denis Villeneuve", "poster", "trailer", AgeRating.T13);
        when(movieRepository.save(any(Movie.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MovieResponse response = movieService.createMovie(request);

        assertEquals("Dune", response.title());
        assertEquals(155, response.durationMinutes());
        assertEquals(AgeRating.T13, response.ageRating());
        verify(movieRepository).save(any(Movie.class));
    }

    @Test
    void updateMovieOnlyChangesNonNullFields() {
        UUID id = UUID.randomUUID();
        Movie movie = movie(id);
        when(movieRepository.findById(id)).thenReturn(Optional.of(movie));
        MovieUpdateRequest request = new MovieUpdateRequest("Dune: Part Two", null, null,
                null, null, null, null, null);

        MovieResponse response = movieService.updateMovie(id, request);

        assertEquals("Dune: Part Two", response.title());
        assertEquals("Original description", response.description());
        assertEquals(120, response.durationMinutes());
        verify(movieRepository, never()).save(any());
    }

    @Test
    void deactivateMoviePerformsSoftDelete() {
        UUID id = UUID.randomUUID();
        Movie movie = movie(id);
        when(movieRepository.findById(id)).thenReturn(Optional.of(movie));

        movieService.deactivateMovie(id);

        assertFalse(movie.isActive());
        verify(movieRepository, never()).delete(any());
        verify(movieRepository, never()).save(any());
    }

    @Test
    void addAndRemoveGenreUpdatesJoinCollection() {
        UUID movieId = UUID.randomUUID();
        UUID genreId = UUID.randomUUID();
        Movie movie = movie(movieId);
        Genre genre = Genre.builder().id(genreId).name("Sci-Fi").active(true).build();
        when(movieRepository.findById(movieId)).thenReturn(Optional.of(movie));
        when(genreRepository.findById(genreId)).thenReturn(Optional.of(genre));

        MovieResponse added = movieService.addGenre(movieId, genreId);
        MovieResponse removed = movieService.removeGenre(movieId, genreId);

        assertEquals(1, added.genres().size());
        assertTrue(removed.genres().isEmpty());
        verify(movieRepository, never()).save(any());
    }

    @Test
    void getMovieThrowsDomainExceptionWhenMissing() {
        UUID id = UUID.randomUUID();
        when(movieRepository.findById(id)).thenReturn(Optional.empty());

        MovieException exception = assertThrows(MovieException.class, () -> movieService.getMovie(id));

        assertEquals("Movie not found", exception.getMessage());
    }

    private Movie movie(UUID id) {
        return Movie.builder()
                .id(id)
                .title("Original title")
                .description("Original description")
                .durationMinutes(120)
                .ageRating(AgeRating.T13)
                .active(true)
                .genres(new LinkedHashSet<>())
                .build();
    }
}
