package congtuong.dev.cinemabooking.payment;

import congtuong.dev.cinemabooking.entity.enums.PaymentProvider;
import congtuong.dev.cinemabooking.exception.PaymentException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentGatewayFactory {

    private final Map<PaymentProvider, PaymentGateway> gateways;

    public PaymentGatewayFactory(List<PaymentGateway> paymentGateways) {
        Map<PaymentProvider, PaymentGateway> registeredGateways =
                new EnumMap<>(PaymentProvider.class);
        for (PaymentGateway paymentGateway : paymentGateways) {
            PaymentGateway previous = registeredGateways.put(
                    paymentGateway.provider(),
                    paymentGateway
            );
            if (previous != null) {
                throw new IllegalStateException(
                        "Duplicate payment gateway: " + paymentGateway.provider()
                );
            }
        }
        gateways = Map.copyOf(registeredGateways);
    }

    public PaymentGateway getGateway(PaymentProvider provider) {
        PaymentGateway gateway = gateways.get(provider);
        if (gateway == null) {
            throw new PaymentException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported payment provider: " + provider
            );
        }
        return gateway;
    }
}
