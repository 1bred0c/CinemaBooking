package congtuong.dev.cinemabooking.ai.indexing;

import congtuong.dev.cinemabooking.entity.Movie;
import congtuong.dev.cinemabooking.repository.MovieRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MovieIndexingServiceImplTest {

    @Mock
    private MovieRepository movieRepository;
    @Mock
    private MovieDocumentFactory documentFactory;
    @Mock
    private VectorStore vectorStore;

    @Test
    void reindexMovieReplacesExistingDocument() {
        UUID movieId = UUID.randomUUID();
        Movie movie = movie(movieId, "Dune");
        Document document = document(movie);
        when(movieRepository.findWithGenresById(movieId))
                .thenReturn(Optional.of(movie));
        when(documentFactory.create(movie)).thenReturn(document);
        MovieIndexingService service = new MovieIndexingServiceImpl(
                movieRepository,
                documentFactory,
                vectorStore
        );

        service.reindexMovie(movieId);

        verify(vectorStore).delete(List.of(movieId.toString()));
        verify(vectorStore).add(List.of(document));
    }

    @Test
    void reindexAllClearsStaleMovieDocumentsFirst() {
        Movie first = movie(UUID.randomUUID(), "Dune");
        Movie second = movie(UUID.randomUUID(), "Arrival");
        when(movieRepository.findAllByActiveTrueOrderByTitleAsc())
                .thenReturn(List.of(first, second));
        when(documentFactory.create(first)).thenReturn(document(first));
        when(documentFactory.create(second)).thenReturn(document(second));
        MovieIndexingService service = new MovieIndexingServiceImpl(
                movieRepository,
                documentFactory,
                vectorStore
        );

        int count = service.reindexAllMovies();

        assertEquals(2, count);
        verify(vectorStore).delete("sourceType == 'MOVIE'");
    }

    private Movie movie(UUID id, String title) {
        return Movie.builder()
                .id(id)
                .title(title)
                .durationMinutes(120)
                .active(true)
                .genres(new LinkedHashSet<>())
                .build();
    }

    private Document document(Movie movie) {
        return Document.builder()
                .id(movie.getId().toString())
                .text("Movie title: " + movie.getTitle())
                .build();
    }
}
