package congtuong.dev.cinemabooking.messaging.outbox;

import congtuong.dev.cinemabooking.entity.OutboxEvent;
import congtuong.dev.cinemabooking.entity.enums.OutboxEventStatus;
import congtuong.dev.cinemabooking.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxPublisherScheduler {

    private final OutboxEventRepository outboxEventRepository;
    private final OutboxPublicationWorker publicationWorker;

    @Scheduled(
            fixedDelayString = "${app.outbox.poll-interval-ms:1000}"
    )
    public void publishPendingEvents() {
        List<UUID> pendingEventIds = outboxEventRepository
                .findTop100ByStatusOrderByOccurredAtAsc(
                        OutboxEventStatus.NEW
                )
                .stream()
                .map(OutboxEvent::getId)
                .toList();

        pendingEventIds.forEach(publicationWorker::publish);
    }
}
