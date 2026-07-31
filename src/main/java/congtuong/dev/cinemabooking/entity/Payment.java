package congtuong.dev.cinemabooking.entity;

import congtuong.dev.cinemabooking.entity.enums.PaymentProvider;
import congtuong.dev.cinemabooking.entity.enums.PaymentStatus;
import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
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
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_booking_idempotency_key",
                        columnNames = {"booking_id", "idempotency_key"}
                ),
                @UniqueConstraint(
                        name = "uk_payment_provider_transaction",
                        columnNames = {"provider", "provider_transaction_id"}
                ),
                @UniqueConstraint(
                        name = "uk_payment_provider_order",
                        columnNames = {"provider", "provider_order_id"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_payment_booking_created_at",
                        columnList = "booking_id,created_at"
                ),
                @Index(
                        name = "idx_payment_status",
                        columnList = "status"
                )
        },
        check = @CheckConstraint(
                name = "ck_payment_amount",
                constraint = "amount > 0"
        )
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentProvider provider;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "provider_order_id", length = 128)
    private String providerOrderId;

    @Column(name = "provider_transaction_id")
    private String providerTransactionId;

    @Column(name = "payment_url", length = 2048)
    private String paymentUrl;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "failed_at")
    private Instant failedAt;

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
            status = PaymentStatus.PENDING;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public void markSucceeded(String providerTransactionId, Instant paidAt) {
        if (status == PaymentStatus.SUCCEEDED) {
            return;
        }
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending payments can succeed"
            );
        }
        status = PaymentStatus.SUCCEEDED;
        this.providerTransactionId = providerTransactionId;
        this.paidAt = paidAt;
        failureReason = null;
        failedAt = null;
    }

    public void markFailed(String failureReason, Instant failedAt) {
        if (status != PaymentStatus.PENDING) {
            return;
        }
        status = PaymentStatus.FAILED;
        this.failureReason = failureReason;
        this.failedAt = failedAt;
    }

    public void markCancelled(String reason) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending payments can be cancelled"
            );
        }
        status = PaymentStatus.CANCELLED;
        failureReason = reason;
    }

    public void markExpired(Instant expiredAt) {
        if (status != PaymentStatus.PENDING) {
            return;
        }
        status = PaymentStatus.EXPIRED;
        failureReason = "Payment period expired";
        failedAt = expiredAt;
    }

    public void initializeProvider(
            String providerOrderId,
            String paymentUrl,
            Instant expiresAt
    ) {
        if (status != PaymentStatus.PENDING) {
            throw new IllegalStateException(
                    "Only pending payments can be initialized"
            );
        }
        assignProviderOrderId(providerOrderId);
        this.paymentUrl = paymentUrl;
        this.expiresAt = expiresAt;
    }

    public void assignProviderOrderId(String providerOrderId) {
        if (this.providerOrderId != null
                && !this.providerOrderId.equals(providerOrderId)) {
            throw new IllegalStateException(
                    "Provider order ID cannot be changed"
            );
        }
        this.providerOrderId = providerOrderId;
    }
}
