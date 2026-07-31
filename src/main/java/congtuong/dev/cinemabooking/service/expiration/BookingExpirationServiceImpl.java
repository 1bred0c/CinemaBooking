package congtuong.dev.cinemabooking.service.expiration;

import congtuong.dev.cinemabooking.entity.Booking;
import congtuong.dev.cinemabooking.entity.enums.BookingStatus;
import congtuong.dev.cinemabooking.repository.BookingRepository;
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
public class BookingExpirationServiceImpl
        implements BookingExpirationService {

    private final BookingRepository bookingRepository;
    private final BookingExpirationWorker expirationWorker;

    @Override
    public int expirePendingBookings() {
        Instant now = Instant.now();
        List<UUID> bookingIds = bookingRepository
                .findTop100ByStatusAndPaymentExpiresAtBeforeOrderByPaymentExpiresAtAsc(
                        BookingStatus.PENDING_PAYMENT,
                        now
                )
                .stream()
                .map(Booking::getId)
                .toList();

        int expiredCount = 0;
        for (UUID bookingId : bookingIds) {
            try {
                if (expirationWorker.expire(bookingId, now)) {
                    expiredCount++;
                }
            } catch (RuntimeException exception) {
                log.error(
                        "Failed to expire bookingId={}",
                        bookingId,
                        exception
                );
            }
        }
        return expiredCount;
    }
}
