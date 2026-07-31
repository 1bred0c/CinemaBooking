package congtuong.dev.cinemabooking.payment.vnpay;

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

import java.math.RoundingMode;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VnPayPaymentGateway implements PaymentGateway {
    private static final ZoneId VNPAY_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter VNPAY_DATE_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final String DEFAULT_CLIENT_IP = "127.0.0.1";

    private final VnPayProperties properties;

    @Override
    public PaymentProvider provider() {
        return PaymentProvider.VNPAY;
    }

    @Override
    public PaymentGatewayResponse createPayment(PaymentGatewayRequest request) {
        validateConfiguration();
        validateRequest(request);

        Instant now = Instant.now();
        Instant expiresAt = request.expiresAt();
        String orderId = providerOrderId(request.paymentId());

        Map<String, String> parameters = new TreeMap<>();
        parameters.put("vnp_Version", properties.version());
        parameters.put("vnp_Command", properties.command());
        parameters.put("vnp_TmnCode", properties.tmnCode());
        parameters.put("vnp_Amount", toVnPayAmount(request));
        parameters.put("vnp_CurrCode", "VND");
        parameters.put("vnp_TxnRef", orderId);
        parameters.put(
                "vnp_OrderInfo",
                "Thanh toan booking " + request.bookingId()
        );
        parameters.put("vnp_OrderType", properties.orderType());
        parameters.put("vnp_Locale", properties.locale());
        parameters.put("vnp_ReturnUrl", properties.returnUrl());
        parameters.put(
                "vnp_IpAddr",
                normalizeClientIp(request.clientIp())
        );
        parameters.put("vnp_CreateDate", format(now));
        parameters.put("vnp_ExpireDate", format(expiresAt));

        String canonicalData = buildCanonicalData(parameters);
        String secureHash = PaymentSignature.hmacHex(
                "HmacSHA512",
                properties.hashSecret(),
                canonicalData
        );
        String paymentUrl = properties.url()
                + "?"
                + canonicalData
                + "&vnp_SecureHash="
                + secureHash;

        return new PaymentGatewayResponse(
                orderId,
                paymentUrl,
                expiresAt
        );
    }

    public boolean verifyCallback(Map<String, String> callbackParameters) {
        String receivedHash = callbackParameters.get("vnp_SecureHash");
        if (isBlank(receivedHash)) {
            return false;
        }

        Map<String, String> signedParameters =
                new HashMap<>(callbackParameters);
        signedParameters.remove("vnp_SecureHash");
        signedParameters.remove("vnp_SecureHashType");
        signedParameters.entrySet().removeIf(
                entry -> !entry.getKey().startsWith("vnp_")
                        || isBlank(entry.getValue())
        );

        String expectedHash = PaymentSignature.hmacHex(
                "HmacSHA512",
                properties.hashSecret(),
                buildCanonicalData(new TreeMap<>(signedParameters))
        );
        return PaymentSignature.constantTimeEquals(
                expectedHash,
                receivedHash
        );
    }

    @Override
    public PaymentGatewayCallback parseCallback(
            Map<String, String> parameters
    ) {
        if (!verifyCallback(parameters)) {
            throw new PaymentException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid VNPay callback signature"
            );
        }
        if (!properties.tmnCode().equals(parameters.get("vnp_TmnCode"))) {
            throw new PaymentException(
                    HttpStatus.BAD_REQUEST,
                    "VNPay callback terminal code does not match"
            );
        }

        try {
            BigDecimal amount = new BigDecimal(
                    required(parameters, "vnp_Amount")
            )
                    .movePointLeft(2);
            boolean successful = "00".equals(parameters.get("vnp_ResponseCode"))
                    && "00".equals(parameters.get("vnp_TransactionStatus"));
            return new PaymentGatewayCallback(
                    required(parameters, "vnp_TxnRef"),
                    parameters.get("vnp_TransactionNo"),
                    amount,
                    successful,
                    parameters.getOrDefault(
                            "vnp_ResponseCode",
                            "Unknown VNPay result"
                    ),
                    Instant.now()
            );
        } catch (NumberFormatException exception) {
            throw new PaymentException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid VNPay callback amount"
            );
        }
    }

    private String buildCanonicalData(Map<String, String> parameters) {
        return parameters.entrySet().stream()
                .filter(entry -> !isBlank(entry.getValue()))
                .map(entry -> encode(entry.getKey())
                        + "="
                        + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String toVnPayAmount(PaymentGatewayRequest request) {
        try {
            return request.amount()
                    .multiply(java.math.BigDecimal.valueOf(100))
                    .setScale(0, RoundingMode.UNNECESSARY)
                    .toBigIntegerExact()
                    .toString();
        } catch (ArithmeticException exception) {
            throw new PaymentException(
                    HttpStatus.BAD_REQUEST,
                    "VNPay amount must contain whole VND"
            );
        }
    }

    private String format(Instant instant) {
        return VNPAY_DATE_FORMAT.format(instant.atZone(VNPAY_ZONE));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
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
                    "Invalid VNPay payment request"
            );
        }
    }

    private void validateConfiguration() {
        if (isBlank(properties.tmnCode())
                || isBlank(properties.hashSecret())
                || isBlank(properties.url())
                || isBlank(properties.returnUrl())
                || isBlank(properties.version())
                || isBlank(properties.command())
                || isBlank(properties.orderType())
                || isBlank(properties.locale())) {
            throw new PaymentException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "VNPay is not configured"
            );
        }
        if (!properties.tmnCode().matches("[A-Za-z0-9]{8}")) {
            throw new PaymentException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "VNPay terminal code must contain exactly 8 "
                            + "alphanumeric characters"
            );
        }
        if (!isHttpUrl(properties.url())
                || !isHttpUrl(properties.returnUrl())) {
            throw new PaymentException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "VNPay payment and return URLs must be valid HTTP URLs"
            );
        }
    }

    private String normalizeClientIp(String clientIp) {
        if (isBlank(clientIp)) {
            return DEFAULT_CLIENT_IP;
        }
        String normalized = clientIp.trim();
        if ("::1".equals(normalized)
                || "0:0:0:0:0:0:0:1".equals(normalized)) {
            return DEFAULT_CLIENT_IP;
        }
        return normalized;
    }

    private boolean isHttpUrl(String value) {
        try {
            URI uri = URI.create(value);
            return ("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (IllegalArgumentException exception) {
            return false;
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
                    "Missing VNPay callback field: " + name
            );
        }
        return value;
    }
}
