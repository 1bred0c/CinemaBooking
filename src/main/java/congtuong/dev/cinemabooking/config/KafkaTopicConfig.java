package congtuong.dev.cinemabooking.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic userEventsTopic(KafkaTopicsProperties topics) {
        return TopicBuilder.name(topics.userEvents())
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    NewTopic bookingEventsTopic(KafkaTopicsProperties topics) {
        return TopicBuilder.name(topics.bookingEvents())
                .partitions(3)
                .replicas(1)
                .build();
    }
}
