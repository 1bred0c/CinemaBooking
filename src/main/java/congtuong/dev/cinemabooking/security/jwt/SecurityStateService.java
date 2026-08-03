package congtuong.dev.cinemabooking.security.jwt;

import congtuong.dev.cinemabooking.entity.User;

import java.util.UUID;

public interface SecurityStateService {
    SecurityState get(UUID userId);

    void cache(User user);

    void advanceVersion(User user);

    String beginTransition(UUID userId);

    boolean isTransitioning(UUID userId);

    void completeTransition(UUID userId, String operationId, SecurityState state);

    void cancelTransition(UUID userId, String operationId);
}
