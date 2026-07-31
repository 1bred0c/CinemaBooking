package congtuong.dev.cinemabooking.dto.response;

import congtuong.dev.cinemabooking.entity.enums.PaymentProvider;
import congtuong.dev.cinemabooking.entity.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID bookingId,
        PaymentProvider provider,
        PaymentStatus status,
        BigDecimal amount,
        String providerOrderId,
        String providerTransactionId,
        String paymentUrl,
        String failureReason,
        Instant expiresAt,
        Instant paidAt,
        Instant failedAt,
        Instant createdAt,
        Instant updatedAt
) {
}
