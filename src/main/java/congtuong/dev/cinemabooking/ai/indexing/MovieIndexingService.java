package congtuong.dev.cinemabooking.ai.indexing;

import java.util.UUID;

public interface MovieIndexingService {
    void reindexMovie(UUID movieId);

    int reindexAllMovies();

    void removeMovie(UUID movieId);
}
