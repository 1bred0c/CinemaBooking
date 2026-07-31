package congtuong.dev.cinemabooking.mapper;

import congtuong.dev.cinemabooking.dto.response.PaymentResponse;
import congtuong.dev.cinemabooking.entity.Payment;
import org.springframework.stereotype.Component;

@Component
public class PaymentMapper {

    public PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getBooking().getId(),
                payment.getProvider(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getProviderOrderId(),
                payment.getProviderTransactionId(),
                payment.getPaymentUrl(),
                payment.getFailureReason(),
                payment.getExpiresAt(),
                payment.getPaidAt(),
                payment.getFailedAt(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }
}
