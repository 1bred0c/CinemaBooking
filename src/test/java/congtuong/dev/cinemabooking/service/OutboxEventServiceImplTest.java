package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.config.KafkaTopicsProperties;
import congtuong.dev.cinemabooking.entity.OutboxEvent;
import congtuong.dev.cinemabooking.entity.enums.OutboxEventStatus;
import congtuong.dev.cinemabooking.messaging.event.UserRegisteredEvent;
import congtuong.dev.cinemabooking.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxEventServiceImplTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Test
    void appendStoresNewUserRegisteredOutboxEvent() {
        ObjectMapper objectMapper = JsonMapper.builder()
                .findAndAddModules()
                .build();
        OutboxEventServiceImpl service = new OutboxEventServiceImpl(
                outboxEventRepository,
                new KafkaTopicsProperties(
                        "cinema.user.events",
                        "cinema.booking.events"
                ),
                objectMapper
        );
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = new UserRegisteredEvent(
                eventId,
                userId,
                "user@example.com",
                "Cinema User",
                Instant.parse("2026-07-30T10:00:00Z"),
                1
        );

        service.append(event);

        ArgumentCaptor<OutboxEvent> captor =
                ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEventRepository).save(captor.capture());

        OutboxEvent stored = captor.getValue();
        assertThat(stored.getId()).isEqualTo(eventId);
        assertThat(stored.getAggregateType()).isEqualTo("USER");
        assertThat(stored.getAggregateId()).isEqualTo(userId);
        assertThat(stored.getEventType()).isEqualTo("USER_REGISTERED");
        assertThat(stored.getTopic()).isEqualTo("cinema.user.events");
        assertThat(stored.getEventKey()).isEqualTo(userId.toString());
        assertThat(stored.getStatus()).isEqualTo(OutboxEventStatus.NEW);
        assertThat(stored.getAttempts()).isZero();
        assertThat(stored.getPayload())
                .contains(eventId.toString())
                .contains("user@example.com");
    }
}
