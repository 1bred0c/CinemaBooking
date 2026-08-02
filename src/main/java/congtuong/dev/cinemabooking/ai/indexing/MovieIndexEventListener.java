package congtuong.dev.cinemabooking.ai.indexing;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class MovieIndexEventListener {

    private final MovieIndexingService movieIndexingService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void synchronize(MovieIndexRequested event) {
        try {
            if (event.action() == MovieIndexAction.REMOVE) {
                movieIndexingService.removeMovie(event.movieId());
            } else {
                movieIndexingService.reindexMovie(event.movieId());
            }
        } catch (RuntimeException exception) {
            log.error(
                    "Movie vector synchronization failed: movieId={}, action={}",
                    event.movieId(),
                    event.action(),
                    exception
            );
        }
    }
}
