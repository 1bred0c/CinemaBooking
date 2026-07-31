package congtuong.dev.cinemabooking.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentGatewayCallback(
        String providerOrderId,
        String providerTransactionId,
        BigDecimal amount,
        boolean successful,
        String message,
        Instant occurredAt
) {
}
