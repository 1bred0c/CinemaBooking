package congtuong.dev.cinemabooking.service;

import java.util.UUID;

public interface UserService {
    void deactivate(UUID userId);
    void activate(UUID userId);
}
