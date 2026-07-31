package congtuong.dev.cinemabooking.dto.request;

import congtuong.dev.cinemabooking.entity.enums.PaymentProvider;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PaymentCreateRequest(
        @NotNull(message = "Booking ID is required")
        UUID bookingId,

        @NotNull(message = "Payment provider is required")
        PaymentProvider provider
) {
}
