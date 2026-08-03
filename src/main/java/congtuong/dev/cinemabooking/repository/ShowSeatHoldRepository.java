package congtuong.dev.cinemabooking.repository;

import congtuong.dev.cinemabooking.entity.ShowSeatHold;
import congtuong.dev.cinemabooking.entity.enums.ShowSeatHoldStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

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

    @Query("""
            select h from ShowSeatHold h
            join fetch h.showtime
            join fetch h.user
            where h.user.id = :userId
              and h.status = :status
              and h.expiresAt > :now
              and (:showtimeId is null or h.showtime.id = :showtimeId)
            order by h.createdAt desc
            """)
    List<ShowSeatHold> findLatestActiveByUser(
            @Param("userId") UUID userId,
            @Param("showtimeId") UUID showtimeId,
            @Param("status") ShowSeatHoldStatus status,
            @Param("now") Instant now,
            Pageable pageable
    );

    List<ShowSeatHold> findAllByStatusAndExpiresAtBefore(
            ShowSeatHoldStatus status,
            Instant time
    );

    List<ShowSeatHold> findTop100ByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
            ShowSeatHoldStatus status,
            Instant time
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select h from ShowSeatHold h
            join fetch h.showtime
            join fetch h.user
            where h.id = :holdId
            """)
    Optional<ShowSeatHold> findByIdForUpdate(
            @Param("holdId") UUID holdId
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
