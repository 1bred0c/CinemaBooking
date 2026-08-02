package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.messaging.event.UserRegisteredEvent;
import congtuong.dev.cinemabooking.messaging.event.BookingNotificationEvent;

public interface OutboxEventService {

    void append(UserRegisteredEvent event);

    void append(BookingNotificationEvent event);
}
