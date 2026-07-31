package congtuong.dev.cinemabooking.dto.response;

import congtuong.dev.cinemabooking.entity.enums.PaymentStatus;

import java.util.UUID;

public record PaymentCallbackResponse(
        UUID paymentId,
        UUID bookingId,
        PaymentStatus status
) {
}
