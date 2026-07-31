package congtuong.dev.cinemabooking.controller;

import congtuong.dev.cinemabooking.dto.request.PaymentCreateRequest;
import congtuong.dev.cinemabooking.dto.response.PaymentResponse;
import congtuong.dev.cinemabooking.dto.response.PaymentCallbackResponse;
import congtuong.dev.cinemabooking.entity.enums.PaymentProvider;
import congtuong.dev.cinemabooking.exception.PaymentException;
import congtuong.dev.cinemabooking.security.jwt.CustomUserDetails;
import congtuong.dev.cinemabooking.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.UUID;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentCreateRequest request,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            HttpServletRequest httpRequest
    ) {
        PaymentResponse response = paymentService.createPayment(
                currentUser.getUser().getId(),
                idempotencyKey,
                resolveClientIp(httpRequest),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPayment(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                paymentService.getPayment(
                        currentUser.getUser().getId(),
                        paymentId
                )
        );
    }

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<PaymentResponse>> getBookingPayments(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                paymentService.getBookingPayments(
                        currentUser.getUser().getId(),
                        bookingId
                )
        );
    }

    @PostMapping("/{paymentId}/cancel")
    public ResponseEntity<PaymentResponse> cancelPendingPayment(
            @PathVariable UUID paymentId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                paymentService.cancelPendingPayment(
                        currentUser.getUser().getId(),
                        paymentId
                )
        );
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<PaymentCallbackResponse> handleVnPayReturn(
            @RequestParam Map<String, String> parameters
    ) {
        return ResponseEntity.ok(paymentService.processCallback(
                PaymentProvider.VNPAY,
                parameters
        ));
    }

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> handleVnPayIpn(
            @RequestParam Map<String, String> parameters
    ) {
        try {
            paymentService.processCallback(
                    PaymentProvider.VNPAY,
                    parameters
            );
            return vnPayIpnResponse("00", "Confirm Success");
        } catch (PaymentException exception) {
            return vnPayIpnError(exception);
        } catch (RuntimeException exception) {
            return vnPayIpnResponse("99", "Unknown error");
        }
    }

    @GetMapping("/momo/return")
    public ResponseEntity<PaymentCallbackResponse> handleMomoReturn(
            @RequestParam Map<String, String> parameters
    ) {
        return ResponseEntity.ok(paymentService.processCallback(
                PaymentProvider.MOMO,
                parameters
        ));
    }

    @PostMapping("/momo/ipn")
    public ResponseEntity<Void> handleMomoIpn(
            @RequestBody Map<String, String> parameters
    ) {
        paymentService.processCallback(PaymentProvider.MOMO, parameters);
        return ResponseEntity.noContent().build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        // Do not trust X-Forwarded-For directly. A trusted reverse proxy
        // should normalize forwarded headers at deployment level.
        return request.getRemoteAddr();
    }

    private ResponseEntity<Map<String, String>> vnPayIpnError(
            PaymentException exception
    ) {
        String message = exception.getMessage();
        if (message != null && (message.contains("signature")
                || message.contains("terminal code"))) {
            return vnPayIpnResponse("97", "Invalid checksum");
        }
        if ("Payment not found".equals(message)) {
            return vnPayIpnResponse("01", "Order not found");
        }
        if (message != null && message.contains("amount")) {
            return vnPayIpnResponse("04", "Invalid amount");
        }
        return vnPayIpnResponse("99", "Unknown error");
    }

    private ResponseEntity<Map<String, String>> vnPayIpnResponse(
            String responseCode,
            String message
    ) {
        return ResponseEntity.ok(Map.of(
                "RspCode", responseCode,
                "Message", message
        ));
    }
}
