package congtuong.dev.cinemabooking.ai.indexing;

import java.util.UUID;

public record MovieIndexRequested(UUID movieId, MovieIndexAction action) {
}
