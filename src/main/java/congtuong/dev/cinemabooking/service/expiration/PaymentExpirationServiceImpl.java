package congtuong.dev.cinemabooking.service.expiration;

import congtuong.dev.cinemabooking.entity.Payment;
import congtuong.dev.cinemabooking.entity.enums.PaymentStatus;
import congtuong.dev.cinemabooking.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class PaymentExpirationServiceImpl
        implements PaymentExpirationService {

    private final PaymentRepository paymentRepository;
    private final PaymentExpirationWorker expirationWorker;

    @Override
    public int expirePendingPayments() {
        Instant now = Instant.now();
        List<UUID> paymentIds = paymentRepository
                .findTop100ByStatusAndExpiresAtBeforeOrderByExpiresAtAsc(
                        PaymentStatus.PENDING,
                        now
                )
                .stream()
                .map(Payment::getId)
                .toList();

        int expiredCount = 0;
        for (UUID paymentId : paymentIds) {
            try {
                if (expirationWorker.expire(paymentId, now)) {
                    expiredCount++;
                }
            } catch (RuntimeException exception) {
                log.error(
                        "Failed to expire paymentId={}",
                        paymentId,
                        exception
                );
            }
        }
        return expiredCount;
    }
}
