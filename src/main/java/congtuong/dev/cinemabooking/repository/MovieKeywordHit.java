package congtuong.dev.cinemabooking.repository;

import java.util.UUID;

public record MovieKeywordHit(UUID movieId, String title, double score) {
}
