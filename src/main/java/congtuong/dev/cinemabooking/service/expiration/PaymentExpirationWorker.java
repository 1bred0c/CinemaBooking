package congtuong.dev.cinemabooking.service.expiration;

import congtuong.dev.cinemabooking.entity.Payment;
import congtuong.dev.cinemabooking.entity.enums.PaymentStatus;
import congtuong.dev.cinemabooking.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentExpirationWorker {

    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean expire(UUID paymentId, Instant now) {
        Payment payment = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElse(null);

        if (payment == null
                || payment.getStatus() != PaymentStatus.PENDING
                || payment.getExpiresAt() == null
                || payment.getExpiresAt().isAfter(now)) {
            return false;
        }

        payment.markExpired(now);
        return true;
    }
}
