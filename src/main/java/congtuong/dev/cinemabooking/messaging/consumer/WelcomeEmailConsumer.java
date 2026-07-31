package congtuong.dev.cinemabooking.messaging.consumer;

import congtuong.dev.cinemabooking.messaging.event.UserRegisteredEvent;
import congtuong.dev.cinemabooking.notification.WelcomeEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
@Slf4j
public class WelcomeEmailConsumer {

    private final ObjectMapper objectMapper;
    private final WelcomeEmailService welcomeEmailService;

    @KafkaListener(
            topics = "${app.kafka.topics.user-events}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consume(String payload) {
        UserRegisteredEvent event =
                objectMapper.readValue(payload, UserRegisteredEvent.class);

        welcomeEmailService.sendWelcomeEmail(
                event.email(),
                event.fullName()
        );

        log.info(
                "Sent welcome email for eventId={} userId={}",
                event.eventId(),
                event.userId()
        );
    }
}
