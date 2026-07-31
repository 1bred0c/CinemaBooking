package congtuong.dev.cinemabooking.repository;

import congtuong.dev.cinemabooking.entity.Payment;
import congtuong.dev.cinemabooking.entity.enums.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    List<Payment> findTop100ByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
            PaymentStatus status,
            Instant time
    );

    @Query("""
            select p from Payment p
            join fetch p.booking b
            where p.id = :paymentId
              and b.user.id = :userId
            """)
    Optional<Payment> findByIdAndBookingUserId(
            @Param("paymentId") UUID paymentId,
            @Param("userId") UUID userId
    );

    @Query("""
            select p from Payment p
            join fetch p.booking b
            where b.id = :bookingId
              and b.user.id = :userId
              and p.idempotencyKey = :idempotencyKey
            """)
    Optional<Payment> findByBookingIdAndUserIdAndIdempotencyKey(
            @Param("bookingId") UUID bookingId,
            @Param("userId") UUID userId,
            @Param("idempotencyKey") String idempotencyKey
    );

    @Query("""
            select p from Payment p
            join fetch p.booking b
            where b.id = :bookingId
              and b.user.id = :userId
            order by p.createdAt desc
            """)
    List<Payment> findAllByBookingIdAndUserId(
            @Param("bookingId") UUID bookingId,
            @Param("userId") UUID userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Payment p
            join fetch p.booking
            where p.id = :paymentId
            """)
    Optional<Payment> findByIdForUpdate(@Param("paymentId") UUID paymentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Payment p
            join fetch p.booking b
            where p.id = :paymentId
              and b.user.id = :userId
            """)
    Optional<Payment> findByIdAndBookingUserIdForUpdate(
            @Param("paymentId") UUID paymentId,
            @Param("userId") UUID userId
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p from Payment p
            join fetch p.booking
            where p.provider = :provider
              and p.providerOrderId = :providerOrderId
            """)
    Optional<Payment> findByProviderAndProviderOrderIdForUpdate(
            @Param("provider")
            congtuong.dev.cinemabooking.entity.enums.PaymentProvider provider,
            @Param("providerOrderId") String providerOrderId
    );

    Optional<Payment>
    findFirstByBookingIdAndStatusOrderByCreatedAtDesc(
            UUID bookingId,
            PaymentStatus status
    );

}
