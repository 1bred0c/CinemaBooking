package congtuong.dev.cinemabooking.messaging.outbox;

import congtuong.dev.cinemabooking.entity.OutboxEvent;
import congtuong.dev.cinemabooking.entity.enums.OutboxEventStatus;
import congtuong.dev.cinemabooking.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxPublicationWorker {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Transactional
    public void publish(UUID eventId) {
        OutboxEvent event = outboxEventRepository
                .findByIdForUpdate(eventId)
                .orElse(null);

        if (event == null
                || event.getStatus() != OutboxEventStatus.NEW) {
            return;
        }

        try {
            var result = kafkaTemplate.send(
                    event.getTopic(),
                    event.getEventKey(),
                    event.getPayload()
            ).get();

            event.markPublished(Instant.now());
            log.info(
                    "Published outbox eventId={} type={} topic={} partition={} offset={}",
                    event.getId(),
                    event.getEventType(),
                    result.getRecordMetadata().topic(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset()
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            event.markFailed(exception.getMessage());
            log.error(
                    "Interrupted while publishing outbox eventId={}",
                    event.getId(),
                    exception
            );
        } catch (ExecutionException exception) {
            event.markFailed(exception.getMessage());
            log.error(
                    "Failed to publish outbox eventId={} attempt={}",
                    event.getId(),
                    event.getAttempts(),
                    exception.getCause()
            );
        } catch (RuntimeException exception) {
            event.markFailed(exception.getMessage());
            log.error(
                    "Failed to publish outbox eventId={} attempt={}",
                    event.getId(),
                    event.getAttempts(),
                    exception
            );
        }
    }
}
