package congtuong.dev.cinemabooking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicsProperties(
        String userEvents,
        String bookingEvents
) {
}
