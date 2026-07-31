package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.PaymentCreateRequest;
import congtuong.dev.cinemabooking.dto.response.PaymentResponse;
import congtuong.dev.cinemabooking.entity.Booking;
import congtuong.dev.cinemabooking.entity.Payment;
import congtuong.dev.cinemabooking.entity.enums.PaymentProvider;
import congtuong.dev.cinemabooking.entity.enums.PaymentStatus;
import congtuong.dev.cinemabooking.exception.PaymentException;
import congtuong.dev.cinemabooking.mapper.PaymentMapper;
import congtuong.dev.cinemabooking.payment.PaymentGatewayFactory;
import congtuong.dev.cinemabooking.repository.BookingItemRepository;
import congtuong.dev.cinemabooking.repository.BookingRepository;
import congtuong.dev.cinemabooking.repository.PaymentRepository;
import congtuong.dev.cinemabooking.repository.ShowSeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceImplTest {

    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingItemRepository bookingItemRepository;
    @Mock
    private ShowSeatRepository showSeatRepository;
    @Mock
    private PaymentGatewayFactory paymentGatewayFactory;
    @Mock
    private PaymentMapper paymentMapper;
    @Mock
    private TransactionTemplate paymentTransactionTemplate;
    @InjectMocks
    private PaymentServiceImpl paymentService;

    @Test
    void sameIdempotencyKeyReturnsExistingInitializedPayment() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Payment payment = payment(bookingId, PaymentProvider.VNPAY);
        payment.setPaymentUrl("https://sandbox.example/pay");
        PaymentResponse expected = response(payment, bookingId);
        PaymentCreateRequest request = new PaymentCreateRequest(
                bookingId,
                PaymentProvider.VNPAY
        );

        when(paymentRepository
                .findByBookingIdAndUserIdAndIdempotencyKey(
                        bookingId,
                        userId,
                        "idem-1"
                ))
                .thenReturn(Optional.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(expected);

        PaymentResponse actual = paymentService.createPayment(
                userId,
                " idem-1 ",
                "127.0.0.1",
                request
        );

        assertSame(expected, actual);
        verify(paymentGatewayFactory, never()).getGateway(
                PaymentProvider.VNPAY
        );
    }

    @Test
    void sameIdempotencyKeyWithDifferentProviderReturnsConflict() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Payment payment = payment(bookingId, PaymentProvider.VNPAY);
        PaymentCreateRequest request = new PaymentCreateRequest(
                bookingId,
                PaymentProvider.MOMO
        );
        when(paymentRepository
                .findByBookingIdAndUserIdAndIdempotencyKey(
                        bookingId,
                        userId,
                        "idem-1"
                ))
                .thenReturn(Optional.of(payment));

        PaymentException exception = assertThrows(
                PaymentException.class,
                () -> paymentService.createPayment(
                        userId,
                        "idem-1",
                        "127.0.0.1",
                        request
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    @Test
    void blankIdempotencyKeyReturnsBadRequest() {
        PaymentException exception = assertThrows(
                PaymentException.class,
                () -> paymentService.createPayment(
                        UUID.randomUUID(),
                        " ",
                        "127.0.0.1",
                        new PaymentCreateRequest(
                                UUID.randomUUID(),
                                PaymentProvider.MOMO
                        )
                )
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        verify(paymentRepository, never())
                .findByBookingIdAndUserIdAndIdempotencyKey(
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    void cancelPendingPaymentAllowsAnotherPaymentAttempt() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Payment payment = payment(bookingId, PaymentProvider.VNPAY);
        PaymentResponse expected = response(payment, bookingId);

        when(paymentRepository.findByIdAndBookingUserIdForUpdate(
                payment.getId(),
                userId
        )).thenReturn(Optional.of(payment));
        when(paymentMapper.toResponse(payment)).thenReturn(expected);

        paymentService.cancelPendingPayment(userId, payment.getId());

        assertEquals(PaymentStatus.CANCELLED, payment.getStatus());
        assertEquals("Cancelled by user", payment.getFailureReason());
    }

    @Test
    void cancellingSucceededPaymentReturnsConflict() {
        UUID userId = UUID.randomUUID();
        UUID bookingId = UUID.randomUUID();
        Payment payment = payment(bookingId, PaymentProvider.VNPAY);
        payment.setStatus(PaymentStatus.SUCCEEDED);
        when(paymentRepository.findByIdAndBookingUserIdForUpdate(
                payment.getId(),
                userId
        )).thenReturn(Optional.of(payment));

        PaymentException exception = assertThrows(
                PaymentException.class,
                () -> paymentService.cancelPendingPayment(
                        userId,
                        payment.getId()
                )
        );

        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
    }

    private Payment payment(
            UUID bookingId,
            PaymentProvider provider
    ) {
        return Payment.builder()
                .id(UUID.randomUUID())
                .booking(Booking.builder().id(bookingId).build())
                .provider(provider)
                .status(PaymentStatus.PENDING)
                .amount(BigDecimal.valueOf(100_000))
                .idempotencyKey("idem-1")
                .expiresAt(Instant.now().plusSeconds(600))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private PaymentResponse response(Payment payment, UUID bookingId) {
        return new PaymentResponse(
                payment.getId(),
                bookingId,
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
