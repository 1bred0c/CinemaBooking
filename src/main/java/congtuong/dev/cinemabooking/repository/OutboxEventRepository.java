package congtuong.dev.cinemabooking.repository;

import congtuong.dev.cinemabooking.entity.OutboxEvent;
import congtuong.dev.cinemabooking.entity.enums.OutboxEventStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OutboxEventRepository
        extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop100ByStatusOrderByOccurredAtAsc(
            OutboxEventStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select event from OutboxEvent event where event.id = :eventId")
    Optional<OutboxEvent> findByIdForUpdate(
            @Param("eventId") UUID eventId
    );
}
