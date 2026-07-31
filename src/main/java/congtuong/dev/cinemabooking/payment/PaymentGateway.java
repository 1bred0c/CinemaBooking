package congtuong.dev.cinemabooking.payment;

import congtuong.dev.cinemabooking.entity.enums.PaymentProvider;
import congtuong.dev.cinemabooking.payment.dto.PaymentGatewayRequest;
import congtuong.dev.cinemabooking.payment.dto.PaymentGatewayResponse;
import congtuong.dev.cinemabooking.payment.dto.PaymentGatewayCallback;

import java.util.Map;
import java.util.UUID;

public interface PaymentGateway {

    PaymentProvider provider();

    default String providerOrderId(UUID paymentId) {
        return paymentId.toString().replace("-", "");
    }

    PaymentGatewayResponse createPayment(PaymentGatewayRequest request);

    PaymentGatewayCallback parseCallback(Map<String, String> parameters);
}
