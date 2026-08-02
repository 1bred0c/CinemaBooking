package congtuong.dev.cinemabooking.messaging.consumer;

import congtuong.dev.cinemabooking.messaging.event.BookingNotificationEvent;
import congtuong.dev.cinemabooking.messaging.event.UserRegisteredEvent;
import congtuong.dev.cinemabooking.notification.NotificationEventService;
import congtuong.dev.cinemabooking.notification.NotificationPushService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class InAppNotificationConsumer {

    private final ObjectMapper objectMapper;
    private final NotificationEventService notificationEventService;
    private final NotificationPushService notificationPushService;

    @KafkaListener(
            topics = "${app.kafka.topics.user-events}",
            groupId = "cinema-in-app-user-notification"
    )
    public void consumeUserEvent(String payload) {
        UserRegisteredEvent event = objectMapper.readValue(
                payload,
                UserRegisteredEvent.class
        );
        notificationEventService.create(event).ifPresent(notification ->
                notificationPushService.send(event.userId(), notification)
        );
    }

    @KafkaListener(
            topics = "${app.kafka.topics.booking-events}",
            groupId = "cinema-in-app-booking-notification"
    )
    public void consumeBookingEvent(String payload) {
        BookingNotificationEvent event = objectMapper.readValue(
                payload,
                BookingNotificationEvent.class
        );
        notificationEventService.create(event).ifPresent(notification ->
                notificationPushService.send(event.userId(), notification)
        );
    }
}
