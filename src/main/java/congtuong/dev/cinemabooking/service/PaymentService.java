package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.PaymentCreateRequest;
import congtuong.dev.cinemabooking.dto.response.PaymentResponse;
import congtuong.dev.cinemabooking.dto.response.PaymentCallbackResponse;
import congtuong.dev.cinemabooking.entity.enums.PaymentProvider;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface PaymentService {

    PaymentResponse createPayment(
            UUID currentUserId,
            String idempotencyKey,
            String clientIp,
            PaymentCreateRequest request
    );

    PaymentResponse getPayment(UUID currentUserId, UUID paymentId);

    List<PaymentResponse> getBookingPayments(
            UUID currentUserId,
            UUID bookingId
    );

    PaymentResponse cancelPendingPayment(
            UUID currentUserId,
            UUID paymentId
    );

    PaymentCallbackResponse processCallback(
            PaymentProvider provider,
            Map<String, String> parameters
    );
}
