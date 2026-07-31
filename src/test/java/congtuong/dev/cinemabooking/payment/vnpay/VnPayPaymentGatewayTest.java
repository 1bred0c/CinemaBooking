package congtuong.dev.cinemabooking.payment.vnpay;

import congtuong.dev.cinemabooking.exception.PaymentException;
import congtuong.dev.cinemabooking.payment.PaymentSignature;
import congtuong.dev.cinemabooking.payment.dto.PaymentGatewayCallback;
import congtuong.dev.cinemabooking.payment.dto.PaymentGatewayRequest;
import congtuong.dev.cinemabooking.payment.dto.PaymentGatewayResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VnPayPaymentGatewayTest {
    private static final String SECRET = "test-secret";

    private final VnPayPaymentGateway gateway = new VnPayPaymentGateway(
            new VnPayProperties(
                    "TESTTMN1",
                    SECRET,
                    "https://sandbox.example/pay",
                    "http://localhost/return",
                    "https://example.test/ipn",
                    "2.1.0",
                    "pay",
                    "other",
                    "vn"
            )
    );

    @Test
    void createPaymentUsesServerAmountAndStableOrderId() {
        UUID paymentId = UUID.randomUUID();
        PaymentGatewayResponse response = gateway.createPayment(
                new PaymentGatewayRequest(
                        paymentId,
                        UUID.randomUUID(),
                        BigDecimal.valueOf(125_000),
                        Instant.now().plusSeconds(600),
                        "127.0.0.1"
                )
        );

        assertEquals(
                paymentId.toString().replace("-", ""),
                response.providerOrderId()
        );
        assertTrue(response.paymentUrl().contains(
                "vnp_Amount=12500000"
        ));
        assertTrue(response.paymentUrl().contains(
                "vnp_TmnCode=TESTTMN1"
        ));

        String rawQuery = URI.create(response.paymentUrl()).getRawQuery();
        String hashSeparator = "&vnp_SecureHash=";
        int hashPosition = rawQuery.lastIndexOf(hashSeparator);
        String signedData = rawQuery.substring(0, hashPosition);
        String emittedHash = rawQuery.substring(
                hashPosition + hashSeparator.length()
        );

        assertEquals(
                PaymentSignature.hmacHex(
                        "HmacSHA512",
                        SECRET,
                        signedData
                ),
                emittedHash
        );
    }

    @Test
    void createPaymentNormalizesIpv6LoopbackForLocalDevelopment() {
        PaymentGatewayResponse response = gateway.createPayment(
                new PaymentGatewayRequest(
                        UUID.randomUUID(),
                        UUID.randomUUID(),
                        BigDecimal.valueOf(125_000),
                        Instant.now().plusSeconds(600),
                        "0:0:0:0:0:0:0:1"
                )
        );

        assertTrue(response.paymentUrl().contains(
                "vnp_IpAddr=127.0.0.1"
        ));
    }

    @Test
    void invalidTerminalCodeIsRejectedBeforeCreatingUrl() {
        VnPayPaymentGateway invalidGateway = new VnPayPaymentGateway(
                new VnPayProperties(
                        "INVALID",
                        SECRET,
                        "https://sandbox.example/pay",
                        "http://localhost/return",
                        "https://example.test/ipn",
                        "2.1.0",
                        "pay",
                        "other",
                        "vn"
                )
        );

        PaymentException exception = assertThrows(
                PaymentException.class,
                () -> invalidGateway.createPayment(
                        new PaymentGatewayRequest(
                                UUID.randomUUID(),
                                UUID.randomUUID(),
                                BigDecimal.valueOf(125_000),
                                Instant.now().plusSeconds(600),
                                "127.0.0.1"
                        )
                )
        );

        assertEquals(
                HttpStatus.INTERNAL_SERVER_ERROR,
                exception.getStatus()
        );
    }

    @Test
    void validSignedCallbackIsParsed() {
        Map<String, String> parameters = new TreeMap<>();
        parameters.put("vnp_TmnCode", "TESTTMN1");
        parameters.put("vnp_TxnRef", "payment-order-1");
        parameters.put("vnp_TransactionNo", "provider-transaction-1");
        parameters.put("vnp_Amount", "12500000");
        parameters.put("vnp_ResponseCode", "00");
        parameters.put("vnp_TransactionStatus", "00");
        parameters.put(
                "vnp_SecureHash",
                PaymentSignature.hmacHex(
                        "HmacSHA512",
                        SECRET,
                        canonicalData(parameters)
                )
        );

        PaymentGatewayCallback callback = gateway.parseCallback(parameters);

        assertTrue(callback.successful());
        assertEquals(
                0,
                BigDecimal.valueOf(125_000).compareTo(callback.amount())
        );
        assertEquals(
                "provider-transaction-1",
                callback.providerTransactionId()
        );
    }

    @Test
    void invalidCallbackSignatureIsRejected() {
        PaymentException exception = assertThrows(
                PaymentException.class,
                () -> gateway.parseCallback(Map.of(
                        "vnp_SecureHash", "invalid"
                ))
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
    }

    private String canonicalData(Map<String, String> parameters) {
        return parameters.entrySet().stream()
                .filter(entry ->
                        !"vnp_SecureHash".equals(entry.getKey())
                                && entry.getValue() != null
                                && !entry.getValue().isBlank()
                )
                .map(entry -> encode(entry.getKey())
                        + "="
                        + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
