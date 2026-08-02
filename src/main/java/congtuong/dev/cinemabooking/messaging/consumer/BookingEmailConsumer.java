package congtuong.dev.cinemabooking.messaging.consumer;

import congtuong.dev.cinemabooking.entity.enums.NotificationType;
import congtuong.dev.cinemabooking.messaging.event.BookingNotificationEvent;
import congtuong.dev.cinemabooking.notification.BookingEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingEmailConsumer {

    private final ObjectMapper objectMapper;
    private final BookingEmailService bookingEmailService;

    @KafkaListener(
            topics = "${app.kafka.topics.booking-events}",
            groupId = "cinema-booking-email"
    )
    public void consume(String payload) {
        BookingNotificationEvent event = objectMapper.readValue(
                payload,
                BookingNotificationEvent.class
        );
        if (event.type() != NotificationType.BOOKING_CONFIRMED) {
            return;
        }
        bookingEmailService.sendBookingConfirmation(event);
        log.info(
                "Sent booking confirmation email eventId={} bookingId={}",
                event.eventId(),
                event.bookingId()
        );
    }
}
