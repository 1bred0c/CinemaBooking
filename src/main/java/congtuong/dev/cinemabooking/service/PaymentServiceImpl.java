package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.PaymentCreateRequest;
import congtuong.dev.cinemabooking.dto.response.PaymentCallbackResponse;
import congtuong.dev.cinemabooking.dto.response.PaymentResponse;
import congtuong.dev.cinemabooking.entity.Booking;
import congtuong.dev.cinemabooking.entity.BookingItem;
import congtuong.dev.cinemabooking.entity.Payment;
import congtuong.dev.cinemabooking.entity.ShowSeat;
import congtuong.dev.cinemabooking.entity.enums.BookingStatus;
import congtuong.dev.cinemabooking.entity.enums.PaymentProvider;
import congtuong.dev.cinemabooking.entity.enums.PaymentStatus;
import congtuong.dev.cinemabooking.entity.enums.ShowSeatStatus;
import congtuong.dev.cinemabooking.exception.PaymentException;
import congtuong.dev.cinemabooking.mapper.PaymentMapper;
import congtuong.dev.cinemabooking.payment.PaymentGateway;
import congtuong.dev.cinemabooking.payment.PaymentGatewayFactory;
import congtuong.dev.cinemabooking.payment.dto.PaymentGatewayCallback;
import congtuong.dev.cinemabooking.payment.dto.PaymentGatewayRequest;
import congtuong.dev.cinemabooking.payment.dto.PaymentGatewayResponse;
import congtuong.dev.cinemabooking.repository.BookingItemRepository;
import congtuong.dev.cinemabooking.repository.BookingRepository;
import congtuong.dev.cinemabooking.repository.PaymentRepository;
import congtuong.dev.cinemabooking.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {
    private static final int MAX_FAILURE_REASON_LENGTH = 255;

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final ShowSeatRepository showSeatRepository;
    private final PaymentGatewayFactory paymentGatewayFactory;
    private final PaymentMapper paymentMapper;
    private final TransactionTemplate paymentTransactionTemplate;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PaymentResponse createPayment(
            UUID currentUserId,
            String idempotencyKey,
            String clientIp,
            PaymentCreateRequest request
    ) {
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        Payment existing = paymentRepository
                .findByBookingIdAndUserIdAndIdempotencyKey(
                        request.bookingId(),
                        currentUserId,
                        normalizedKey
                )
                .orElse(null);

        if (existing != null) {
            validateSameOperation(existing, request);
            if (!needsProviderInitialization(existing)) {
                return paymentMapper.toResponse(existing);
            }
            return initializeWithProvider(existing.getId(), clientIp);
        }

        UUID paymentId = Objects.requireNonNull(
                paymentTransactionTemplate.execute(status ->
                        createPendingAttempt(
                                currentUserId,
                                normalizedKey,
                                request
                        )
                )
        );
        return initializeWithProvider(paymentId, clientIp);
    }

    @Override
    public PaymentResponse getPayment(UUID currentUserId, UUID paymentId) {
        Payment payment = paymentRepository
                .findByIdAndBookingUserId(paymentId, currentUserId)
                .orElseThrow(() -> new PaymentException("Payment not found"));
        return paymentMapper.toResponse(payment);
    }

    @Override
    public List<PaymentResponse> getBookingPayments(
            UUID currentUserId,
            UUID bookingId
    ) {
        return paymentRepository
                .findAllByBookingIdAndUserId(bookingId, currentUserId)
                .stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public PaymentResponse cancelPendingPayment(
            UUID currentUserId,
            UUID paymentId
    ) {
        Payment payment = paymentRepository
                .findByIdAndBookingUserIdForUpdate(
                        paymentId,
                        currentUserId
                )
                .orElseThrow(() ->
                        new PaymentException("Payment not found")
                );

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentException(
                    HttpStatus.CONFLICT,
                    "Only pending payments can be cancelled"
            );
        }

        payment.markCancelled("Cancelled by user");
        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PaymentCallbackResponse processCallback(
            PaymentProvider provider,
            Map<String, String> parameters
    ) {
        PaymentGatewayCallback callback = paymentGatewayFactory
                .getGateway(provider)
                .parseCallback(parameters);

        return Objects.requireNonNull(
                paymentTransactionTemplate.execute(status ->
                        applyCallback(provider, callback)
                )
        );
    }

    private UUID createPendingAttempt(
            UUID currentUserId,
            String idempotencyKey,
            PaymentCreateRequest request
    ) {
        Booking booking = bookingRepository
                .findByIdAndUserIdForUpdate(
                        request.bookingId(),
                        currentUserId
                )
                .orElseThrow(() -> new PaymentException("Booking not found"));

        Payment repeatedRequest = paymentRepository
                .findByBookingIdAndUserIdAndIdempotencyKey(
                        request.bookingId(),
                        currentUserId,
                        idempotencyKey
                )
                .orElse(null);
        if (repeatedRequest != null) {
            validateSameOperation(repeatedRequest, request);
            return repeatedRequest.getId();
        }

        validatePayableBooking(booking);
        Payment pendingAttempt = paymentRepository
                .findFirstByBookingIdAndStatusOrderByCreatedAtDesc(
                        booking.getId(),
                        PaymentStatus.PENDING
                )
                .orElse(null);
        if (pendingAttempt != null) {
            if (pendingAttempt.getExpiresAt() != null
                    && !pendingAttempt.getExpiresAt().isAfter(Instant.now())) {
                pendingAttempt.markFailed(
                        "Payment attempt expired",
                        Instant.now()
                );
            } else {
                throw new PaymentException(
                        HttpStatus.CONFLICT,
                        "Another payment attempt is already pending"
                );
            }
        }

        Payment payment = Payment.builder()
                .booking(booking)
                .provider(request.provider())
                .status(PaymentStatus.PENDING)
                .amount(booking.getTotalAmount())
                .idempotencyKey(idempotencyKey)
                .expiresAt(booking.getPaymentExpiresAt())
                .build();
        paymentRepository.save(payment);
        payment.assignProviderOrderId(
                paymentGatewayFactory
                        .getGateway(request.provider())
                        .providerOrderId(payment.getId())
        );
        paymentRepository.flush();
        return payment.getId();
    }

    private PaymentResponse initializeWithProvider(
            UUID paymentId,
            String clientIp
    ) {
        ProviderInitialization initialization = Objects.requireNonNull(
                paymentTransactionTemplate.execute(status -> {
                    Payment payment = paymentRepository
                            .findByIdForUpdate(paymentId)
                            .orElseThrow(() ->
                                    new PaymentException("Payment not found")
                            );
                    if (!needsProviderInitialization(payment)) {
                        return new ProviderInitialization(
                                null,
                                null,
                                paymentMapper.toResponse(payment)
                        );
                    }
                    if (payment.getProviderOrderId() == null) {
                        payment.assignProviderOrderId(
                                paymentGatewayFactory
                                        .getGateway(payment.getProvider())
                                        .providerOrderId(payment.getId())
                        );
                    }
                    return new ProviderInitialization(
                            payment.getProvider(),
                            new PaymentGatewayRequest(
                                    payment.getId(),
                                    payment.getBooking().getId(),
                                    payment.getAmount(),
                                    payment.getExpiresAt(),
                                    clientIp
                            ),
                            null
                    );
                })
        );
        if (initialization.existingResponse() != null) {
            return initialization.existingResponse();
        }

        PaymentGatewayResponse gatewayResponse;
        try {
            gatewayResponse = paymentGatewayFactory
                    .getGateway(initialization.provider())
                    .createPayment(initialization.request());
        } catch (PaymentException exception) {
            // A 502 means the request outcome may be unknown (timeout,
            // malformed response, or provider-side error). Keep the attempt
            // PENDING so retrying the same idempotency key reuses the same
            // provider order ID instead of risking a duplicate charge.
            if (exception.getStatus() != HttpStatus.BAD_GATEWAY) {
                markInitializationFailed(paymentId, exception.getMessage());
            }
            throw exception;
        } catch (RuntimeException exception) {
            markInitializationFailed(paymentId, exception.getMessage());
            throw exception;
        }

        return Objects.requireNonNull(
                paymentTransactionTemplate.execute(status -> {
                    Payment lockedPayment = paymentRepository
                            .findByIdForUpdate(paymentId)
                            .orElseThrow(() ->
                                    new PaymentException("Payment not found")
                            );
                    if (needsProviderInitialization(lockedPayment)) {
                        lockedPayment.initializeProvider(
                                gatewayResponse.providerOrderId(),
                                gatewayResponse.paymentUrl(),
                                gatewayResponse.expiresAt()
                        );
                    }
                    return paymentMapper.toResponse(lockedPayment);
                })
        );
    }

    private void markInitializationFailed(UUID paymentId, String reason) {
        paymentTransactionTemplate.executeWithoutResult(status ->
                paymentRepository.findByIdForUpdate(paymentId)
                        .filter(payment ->
                                payment.getStatus() == PaymentStatus.PENDING
                                        && payment.getPaymentUrl() == null
                        )
                        .ifPresent(payment -> payment.markFailed(
                                truncateReason(reason),
                                Instant.now()
                        ))
        );
    }

    private PaymentCallbackResponse applyCallback(
            PaymentProvider provider,
            PaymentGatewayCallback callback
    ) {
        Payment payment = paymentRepository
                .findByProviderAndProviderOrderIdForUpdate(
                        provider,
                        callback.providerOrderId()
                )
                .orElseThrow(() -> new PaymentException("Payment not found"));

        if (payment.getAmount().compareTo(callback.amount()) != 0) {
            throw new PaymentException(
                    HttpStatus.CONFLICT,
                    "Callback amount does not match payment amount"
            );
        }
        if (payment.getStatus() == PaymentStatus.SUCCEEDED) {
            validateTransactionIdentity(payment, callback);
            return toCallbackResponse(payment);
        }
        if (payment.getStatus() != PaymentStatus.PENDING) {
            return toCallbackResponse(payment);
        }
        if (!callback.successful()) {
            payment.markFailed(
                    truncateReason(callback.message()),
                    callback.occurredAt()
            );
            return toCallbackResponse(payment);
        }
        if (callback.providerTransactionId() == null
                || callback.providerTransactionId().isBlank()) {
            throw new PaymentException(
                    HttpStatus.BAD_REQUEST,
                    "Missing provider transaction ID"
            );
        }

        Booking booking = bookingRepository
                .findByIdForUpdate(payment.getBooking().getId())
                .orElseThrow(() -> new PaymentException("Booking not found"));
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new PaymentException(
                    HttpStatus.CONFLICT,
                    "Booking is no longer pending payment"
            );
        }

        List<BookingItem> bookingItems =
                bookingItemRepository.findAllByBookingId(booking.getId());
        List<UUID> showSeatIds = bookingItems.stream()
                .map(item -> item.getShowSeat().getId())
                .sorted()
                .toList();
        if (showSeatIds.isEmpty()) {
            throw new PaymentException(
                    HttpStatus.CONFLICT,
                    "Booking does not contain any seats"
            );
        }
        List<ShowSeat> showSeats =
                showSeatRepository.findAllByIdForUpdate(showSeatIds);
        if (showSeats.size() != showSeatIds.size()
                || showSeats.stream().anyMatch(showSeat ->
                        showSeat.getStatus() != ShowSeatStatus.HELD
                )) {
            throw new PaymentException(
                    HttpStatus.CONFLICT,
                    "One or more booking seats are no longer held"
            );
        }

        showSeats.forEach(showSeat ->
                showSeat.setStatus(ShowSeatStatus.BOOKED)
        );
        payment.markSucceeded(
                callback.providerTransactionId(),
                callback.occurredAt()
        );
        booking.markConfirmed(callback.occurredAt());
        return toCallbackResponse(payment);
    }

    private void validatePayableBooking(Booking booking) {
        if (booking.getStatus() != BookingStatus.PENDING_PAYMENT) {
            throw new PaymentException(
                    HttpStatus.CONFLICT,
                    "Booking is not pending payment"
            );
        }
        if (booking.getPaymentExpiresAt() == null
                || !booking.getPaymentExpiresAt().isAfter(Instant.now())) {
            throw new PaymentException(
                    HttpStatus.CONFLICT,
                    "Booking payment period has expired"
            );
        }
    }

    private boolean needsProviderInitialization(Payment payment) {
        return payment.getStatus() == PaymentStatus.PENDING
                && payment.getPaymentUrl() == null;
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        if (idempotencyKey == null) {
            throw new PaymentException(
                    HttpStatus.BAD_REQUEST,
                    "Idempotency-Key is required"
            );
        }
        String normalizedKey = idempotencyKey.trim();
        if (normalizedKey.isEmpty() || normalizedKey.length() > 128) {
            throw new PaymentException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid Idempotency-Key"
            );
        }
        return normalizedKey;
    }

    private void validateSameOperation(
            Payment payment,
            PaymentCreateRequest request
    ) {
        if (payment.getProvider() != request.provider()) {
            throw new PaymentException(
                    HttpStatus.CONFLICT,
                    "Idempotency key was used with another provider"
            );
        }
    }

    private void validateTransactionIdentity(
            Payment payment,
            PaymentGatewayCallback callback
    ) {
        if (callback.providerTransactionId() != null
                && payment.getProviderTransactionId() != null
                && !payment.getProviderTransactionId().equals(
                        callback.providerTransactionId()
                )) {
            throw new PaymentException(
                    HttpStatus.CONFLICT,
                    "Callback transaction does not match payment"
            );
        }
    }

    private PaymentCallbackResponse toCallbackResponse(Payment payment) {
        return new PaymentCallbackResponse(
                payment.getId(),
                payment.getBooking().getId(),
                payment.getStatus()
        );
    }

    private String truncateReason(String reason) {
        String safeReason = reason == null || reason.isBlank()
                ? "Payment provider request failed"
                : reason;
        return safeReason.length() <= MAX_FAILURE_REASON_LENGTH
                ? safeReason
                : safeReason.substring(0, MAX_FAILURE_REASON_LENGTH);
    }

    private record ProviderInitialization(
            PaymentProvider provider,
            PaymentGatewayRequest request,
            PaymentResponse existingResponse
    ) {
    }
}
