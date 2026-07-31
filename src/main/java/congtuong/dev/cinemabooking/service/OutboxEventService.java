package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.messaging.event.UserRegisteredEvent;

public interface OutboxEventService {

    void append(UserRegisteredEvent event);
}
