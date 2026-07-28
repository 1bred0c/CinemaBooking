package congtuong.dev.cinemabooking.repository;

import congtuong.dev.cinemabooking.entity.ShowSeatHold;
import congtuong.dev.cinemabooking.entity.enums.ShowSeatHoldStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShowSeatHoldRepository extends JpaRepository<ShowSeatHold, UUID> {

    @Query("""
            select h from ShowSeatHold h
            join fetch h.showtime
            join fetch h.user
            where h.id = :holdId
              and h.user.id = :userId
            """)
    Optional<ShowSeatHold> findByIdAndUserId(
            @Param("holdId") UUID holdId,
            @Param("userId") UUID userId
    );

    List<ShowSeatHold> findAllByStatusAndExpiresAtBefore(
            ShowSeatHoldStatus status,
            Instant time
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select h from ShowSeatHold h
            join fetch h.showtime
            join fetch h.user
            where h.id = :holdId
              and h.user.id = :userId
            """)
    Optional<ShowSeatHold> findByIdAndUserIdForUpdate(
            @Param("holdId") UUID holdId,
            @Param("userId") UUID userId
    );
}
