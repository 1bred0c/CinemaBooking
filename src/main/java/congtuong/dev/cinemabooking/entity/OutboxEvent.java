package congtuong.dev.cinemabooking.entity;

import congtuong.dev.cinemabooking.entity.enums.OutboxEventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "outbox_events",
        indexes = @Index(
                name = "idx_outbox_status_occurred_at",
                columnList = "status, occurred_at"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxEvent {

    private static final int MAX_ERROR_LENGTH = 1000;

    @Id
    private UUID id;

    @Column(name = "aggregate_type", nullable = false, length = 50)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false)
    private String topic;

    @Column(name = "event_key", nullable = false)
    private String eventKey;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    public static OutboxEvent create(
            UUID eventId,
            String aggregateType,
            UUID aggregateId,
            String eventType,
            String topic,
            String eventKey,
            String payload,
            Instant occurredAt
    ) {
        OutboxEvent event = new OutboxEvent();
        event.id = eventId;
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.eventType = eventType;
        event.topic = topic;
        event.eventKey = eventKey;
        event.payload = payload;
        event.status = OutboxEventStatus.NEW;
        event.occurredAt = occurredAt;
        event.attempts = 0;
        return event;
    }

    public void markPublished(Instant publishedAt) {
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = publishedAt;
        this.lastError = null;
    }

    public void markFailed(String error) {
        this.attempts++;
        String safeError = error == null || error.isBlank()
                ? "Unknown Kafka publishing error"
                : error;
        this.lastError = safeError.length() <= MAX_ERROR_LENGTH
                ? safeError
                : safeError.substring(0, MAX_ERROR_LENGTH);
    }
}
