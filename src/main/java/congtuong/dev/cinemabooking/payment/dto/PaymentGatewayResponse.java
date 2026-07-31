package congtuong.dev.cinemabooking.payment.dto;

import java.time.Instant;

public record PaymentGatewayResponse(
        String providerOrderId,
        String paymentUrl,
        Instant expiresAt
) {
}
