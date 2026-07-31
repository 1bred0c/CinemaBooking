package congtuong.dev.cinemabooking.payment.momo;

import congtuong.dev.cinemabooking.entity.enums.PaymentProvider;
import congtuong.dev.cinemabooking.exception.PaymentException;
import congtuong.dev.cinemabooking.payment.PaymentGateway;
import congtuong.dev.cinemabooking.payment.PaymentSignature;
import congtuong.dev.cinemabooking.payment.dto.PaymentGatewayRequest;
import congtuong.dev.cinemabooking.payment.dto.PaymentGatewayResponse;
import congtuong.dev.cinemabooking.payment.dto.PaymentGatewayCallback;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.RoundingMode;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class MomoPaymentGateway implements PaymentGateway {
    private static final String EMPTY_EXTRA_DATA = "";

    private final MomoProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient paymentHttpClient;

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.MOMO;
    }

    @Override
    public PaymentGatewayResponse createPayment(PaymentGatewayRequest request) {
        validateConfiguration();
        validateRequest(request);

        String providerOrderId = toProviderOrderId(request);
        String requestId = providerOrderId;
        long amount = toMomoAmount(request);
        String orderInfo = "Thanh toan booking " + request.bookingId();

        String rawSignature = buildCreateSignatureRaw(
                amount,
                providerOrderId,
                orderInfo,
                requestId
        );
        String signature = PaymentSignature.hmacHex(
                "HmacSHA256",
                properties.getSecretKey(),
                rawSignature
        );

        MomoCreatePaymentRequest providerRequest =
                new MomoCreatePaymentRequest(
                        properties.getPartnerCode(),
                        properties.getPartnerName(),
                        properties.getStoreId(),
                        requestId,
                        amount,
                        providerOrderId,
                        orderInfo,
                        properties.getRedirectUrl(),
                        properties.getIpnUrl(),
                        properties.getLang(),
                        properties.getRequestType(),
                        true,
                        EMPTY_EXTRA_DATA,
                        signature
                );

        MomoCreatePaymentResponse providerResponse =
                sendCreatePayment(providerRequest);
        if (providerResponse.resultCode() == null
                || providerResponse.resultCode() != 0
                || isBlank(providerResponse.payUrl())) {
            throw new PaymentException(
                    HttpStatus.UNPROCESSABLE_CONTENT,
                    "MoMo rejected payment initialization: "
                            + providerResponse.message()
            );
        }
        if (!providerOrderId.equals(providerResponse.orderId())
                || !requestId.equals(providerResponse.requestId())) {
            throw new PaymentException(
                    HttpStatus.BAD_GATEWAY,
                    "MoMo returned mismatched payment identifiers"
            );
        }
        if (providerResponse.amount() == null
                || providerResponse.amount() != amount) {
            throw new PaymentException(
                    HttpStatus.BAD_GATEWAY,
                    "MoMo returned mismatched payment amount"
            );
        }

        return new PaymentGatewayResponse(
                providerOrderId,
                providerResponse.payUrl(),
                request.expiresAt()
        );
    }

    public boolean verifyCallback(Map<String, String> callbackParameters) {
        String receivedSignature = callbackParameters.get("signature");
        if (isBlank(receivedSignature)) {
            return false;
        }

        String rawSignature = buildRawSignature(
                "accessKey",
                properties.getAccessKey(),
                "amount",
                callbackParameters.getOrDefault("amount", ""),
                "extraData",
                callbackParameters.getOrDefault("extraData", ""),
                "message",
                callbackParameters.getOrDefault("message", ""),
                "orderId",
                callbackParameters.getOrDefault("orderId", ""),
                "orderInfo",
                callbackParameters.getOrDefault("orderInfo", ""),
                "orderType",
                callbackParameters.getOrDefault("orderType", ""),
                "partnerCode",
                callbackParameters.getOrDefault("partnerCode", ""),
                "payType",
                callbackParameters.getOrDefault("payType", ""),
                "requestId",
                callbackParameters.getOrDefault("requestId", ""),
                "responseTime",
                callbackParameters.getOrDefault("responseTime", ""),
                "resultCode",
                callbackParameters.getOrDefault("resultCode", ""),
                "transId",
                callbackParameters.getOrDefault("transId", "")
        );
        String expectedSignature = PaymentSignature.hmacHex(
                "HmacSHA256",
                properties.getSecretKey(),
                rawSignature
        );
        return PaymentSignature.constantTimeEquals(
                expectedSignature,
                receivedSignature
        );
    }

    @Override
    public PaymentGatewayCallback parseCallback(
            Map<String, String> parameters
    ) {
        if (!verifyCallback(parameters)) {
            throw new PaymentException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid MoMo callback signature"
            );
        }
        if (!properties.getPartnerCode().equals(parameters.get("partnerCode"))) {
            throw new PaymentException(
                    HttpStatus.BAD_REQUEST,
                    "MoMo callback partner code does not match"
            );
        }
        try {
            return new PaymentGatewayCallback(
                    required(parameters, "orderId"),
                    parameters.get("transId"),
                    new BigDecimal(required(parameters, "amount")),
                    "0".equals(parameters.get("resultCode")),
                    parameters.getOrDefault("message", "Unknown MoMo result"),
                    Instant.now()
            );
        } catch (NumberFormatException exception) {
            throw new PaymentException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid MoMo callback amount"
            );
        }
    }

    private MomoCreatePaymentResponse sendCreatePayment(
            MomoCreatePaymentRequest providerRequest
    ) {
        try {
            String requestBody =
                    objectMapper.writeValueAsString(providerRequest);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(properties.getEndpoint()))
                    .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            requestBody,
                            StandardCharsets.UTF_8
                    ))
                    .build();

            HttpResponse<String> response = paymentHttpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new PaymentException(
                        HttpStatus.BAD_GATEWAY,
                        "MoMo returned HTTP " + response.statusCode()
                );
            }
            return objectMapper.readValue(
                    response.body(),
                    MomoCreatePaymentResponse.class
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PaymentException(
                    HttpStatus.BAD_GATEWAY,
                    "MoMo payment request was interrupted"
            );
        } catch (IOException | IllegalArgumentException exception) {
            throw new PaymentException(
                    HttpStatus.BAD_GATEWAY,
                    "Unable to initialize MoMo payment"
            );
        }
    }

    private String buildCreateSignatureRaw(
            long amount,
            String orderId,
            String orderInfo,
            String requestId
    ) {
        return "accessKey=" + properties.getAccessKey()
                + "&amount=" + amount
                + "&extraData=" + EMPTY_EXTRA_DATA
                + "&ipnUrl=" + properties.getIpnUrl()
                + "&orderId=" + orderId
                + "&orderInfo=" + orderInfo
                + "&partnerCode=" + properties.getPartnerCode()
                + "&redirectUrl=" + properties.getRedirectUrl()
                + "&requestId=" + requestId
                + "&requestType=" + properties.getRequestType();
    }

    private String buildRawSignature(String... keyValues) {
        StringBuilder rawSignature = new StringBuilder();
        for (int index = 0; index < keyValues.length; index += 2) {
            if (!rawSignature.isEmpty()) {
                rawSignature.append('&');
            }
            rawSignature.append(keyValues[index])
                    .append('=')
                    .append(keyValues[index + 1]);
        }
        return rawSignature.toString();
    }

    private long toMomoAmount(PaymentGatewayRequest request) {
        try {
            return request.amount()
                    .setScale(0, RoundingMode.UNNECESSARY)
                    .longValueExact();
        } catch (ArithmeticException exception) {
            throw new PaymentException(
                    HttpStatus.BAD_REQUEST,
                    "MoMo amount must contain whole VND"
            );
        }
    }

    private String toProviderOrderId(PaymentGatewayRequest request) {
        return providerOrderId(request.paymentId());
    }

    private void validateRequest(PaymentGatewayRequest request) {
        if (request == null
                || request.paymentId() == null
                || request.bookingId() == null
                || request.amount() == null
                || request.amount().signum() <= 0
                || request.expiresAt() == null
                || !request.expiresAt().isAfter(Instant.now())) {
            throw new PaymentException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid MoMo payment request"
            );
        }
    }

    private void validateConfiguration() {
        if (isBlank(properties.getEndpoint())
                || isBlank(properties.getPartnerCode())
                || isBlank(properties.getAccessKey())
                || isBlank(properties.getSecretKey())
                || isBlank(properties.getRedirectUrl())
                || isBlank(properties.getIpnUrl())
                || isBlank(properties.getRequestType())
                || isBlank(properties.getLang())
                || properties.getTimeoutSeconds() <= 0) {
            throw new PaymentException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "MoMo is not configured"
            );
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String required(Map<String, String> parameters, String name) {
        String value = parameters.get(name);
        if (isBlank(value)) {
            throw new PaymentException(
                    HttpStatus.BAD_REQUEST,
                    "Missing MoMo callback field: " + name
            );
        }
        return value;
    }
}
