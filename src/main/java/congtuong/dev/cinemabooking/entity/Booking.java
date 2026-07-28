package congtuong.dev.cinemabooking.entity;

import congtuong.dev.cinemabooking.entity.enums.BookingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "bookings",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_booking_hold",
                columnNames = "hold_id"
        ),
        indexes = {
                @Index(
                        name = "idx_booking_user_created_at",
                        columnList = "user_id,created_at"
                ),
                @Index(
                        name = "idx_booking_status_payment_expires_at",
                        columnList = "status,payment_expires_at"
                )
        },
        check = @CheckConstraint(
                name = "ck_booking_total_amount",
                constraint = "total_amount >= 0"
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Booking {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id", nullable = false)
    private ShowTime showtime;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hold_id", nullable = false)
    private ShowSeatHold hold;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_expires_at", nullable = false)
    private Instant paymentExpiresAt;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = BookingStatus.PENDING_PAYMENT;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void markConfirmed(Instant confirmedAt) {
        // TODO BOOKING STATE: Add strict state-transition validation later.
        status = BookingStatus.CONFIRMED;
        this.confirmedAt = confirmedAt;
    }

    public void markCancelled(Instant cancelledAt) {
        // TODO BOOKING STATE: Add strict state-transition validation later.
        status = BookingStatus.CANCELLED;
        this.cancelledAt = cancelledAt;
    }

    public void markExpired() {
        // TODO BOOKING STATE: Add strict state-transition validation later.
        status = BookingStatus.EXPIRED;
    }
}
