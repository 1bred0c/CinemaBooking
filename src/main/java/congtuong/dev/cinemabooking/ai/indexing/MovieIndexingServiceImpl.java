package congtuong.dev.cinemabooking.ai.indexing;

import congtuong.dev.cinemabooking.entity.Movie;
import congtuong.dev.cinemabooking.exception.MovieException;
import congtuong.dev.cinemabooking.repository.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieIndexingServiceImpl implements MovieIndexingService {

    private final MovieRepository movieRepository;
    private final MovieDocumentFactory documentFactory;
    private final VectorStore vectorStore;

    @Override
    @Transactional
    public void reindexMovie(UUID movieId) {
        Movie movie = movieRepository.findWithGenresById(movieId)
                .orElseThrow(() -> new MovieException("Movie not found"));

        removeMovie(movieId);
        if (movie.isActive()) {
            vectorStore.add(List.of(documentFactory.create(movie)));
        }
    }

    @Override
    @Transactional
    public int reindexAllMovies() {
        List<Movie> movies = movieRepository.findAllByActiveTrueOrderByTitleAsc();
        vectorStore.delete("sourceType == 'MOVIE'");
        if (!movies.isEmpty()) {
            vectorStore.add(movies.stream()
                    .map(documentFactory::create)
                    .toList());
        }
        return movies.size();
    }

    @Override
    @Transactional
    public void removeMovie(UUID movieId) {
        vectorStore.delete(List.of(documentId(movieId)));
    }

    private String documentId(UUID movieId) {
        return movieId.toString();
    }
}
