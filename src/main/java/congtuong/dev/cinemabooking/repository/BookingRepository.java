package congtuong.dev.cinemabooking.repository;

import congtuong.dev.cinemabooking.entity.Booking;
import congtuong.dev.cinemabooking.entity.enums.BookingStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    List<Booking>
    findTop100ByStatusAndPaymentExpiresAtBeforeOrderByPaymentExpiresAtAsc(
            BookingStatus status,
            Instant time
    );

    @Query("""
            select b from Booking b
            join fetch b.user
            join fetch b.showtime
            join fetch b.hold
            where b.id = :bookingId
              and b.user.id = :userId
            """)
    Optional<Booking> findByIdAndUserId(
            @Param("bookingId") UUID bookingId,
            @Param("userId") UUID userId
    );

    Optional<Booking> findByHoldId(UUID holdId);

    boolean existsByHoldId(UUID holdId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from Booking b
            join fetch b.user
            join fetch b.showtime
            join fetch b.hold
            where b.id = :bookingId
            """)
    Optional<Booking> findByIdForUpdate(@Param("bookingId") UUID bookingId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select b from Booking b
            join fetch b.user
            join fetch b.showtime
            join fetch b.hold
            where b.id = :bookingId
              and b.user.id = :userId
            """)
    Optional<Booking> findByIdAndUserIdForUpdate(
            @Param("bookingId") UUID bookingId,
            @Param("userId") UUID userId
    );

    @Query("""
            select b from Booking b
            join fetch b.showtime
            where b.user.id = :userId
            order by b.createdAt desc
            """)
    List<Booking> findAllByUserIdOrderByCreatedAtDesc(
            @Param("userId") UUID userId
    );

    @Query("""
            select b from Booking b
            join fetch b.showtime st
            join fetch st.movie
            join fetch st.room r
            join fetch r.cinema
            where b.user.id = :userId
              and (:status is null or b.status = :status)
            order by b.createdAt desc
            """)
    List<Booking> findMyBookings(
            @Param("userId") UUID userId,
            @Param("status") BookingStatus status
    );
}
