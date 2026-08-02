package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.config.KafkaTopicsProperties;
import congtuong.dev.cinemabooking.entity.OutboxEvent;
import congtuong.dev.cinemabooking.messaging.event.UserRegisteredEvent;
import congtuong.dev.cinemabooking.messaging.event.BookingNotificationEvent;
import congtuong.dev.cinemabooking.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OutboxEventServiceImpl implements OutboxEventService {

    private static final String USER_AGGREGATE = "USER";
    private static final String USER_REGISTERED = "USER_REGISTERED";
    private static final String BOOKING_AGGREGATE = "BOOKING";

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTopicsProperties topics;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void append(UserRegisteredEvent event) {
        String payload = objectMapper.writeValueAsString(event);

        outboxEventRepository.save(
                OutboxEvent.create(
                        event.eventId(),
                        USER_AGGREGATE,
                        event.userId(),
                        USER_REGISTERED,
                        topics.userEvents(),
                        event.userId().toString(),
                        payload,
                        event.occurredAt()
                )
        );
    }

    @Override
    @Transactional
    public void append(BookingNotificationEvent event) {
        String payload = objectMapper.writeValueAsString(event);

        outboxEventRepository.save(
                OutboxEvent.create(
                        event.eventId(),
                        BOOKING_AGGREGATE,
                        event.bookingId(),
                        event.type().name(),
                        topics.bookingEvents(),
                        event.bookingId().toString(),
                        payload,
                        event.occurredAt()
                )
        );
    }
}
