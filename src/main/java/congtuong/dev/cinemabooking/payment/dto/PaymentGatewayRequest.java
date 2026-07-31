package congtuong.dev.cinemabooking.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentGatewayRequest(
        UUID paymentId,
        UUID bookingId,
        BigDecimal amount,
        Instant expiresAt,
        String clientIp
) {
}
