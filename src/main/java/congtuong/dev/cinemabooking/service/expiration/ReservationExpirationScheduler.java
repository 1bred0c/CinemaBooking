package congtuong.dev.cinemabooking.service.expiration;

import congtuong.dev.cinemabooking.service.ShowSeatHoldService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        prefix = "booking",
        name = "expiration-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ReservationExpirationScheduler {

    private final ShowSeatHoldService showSeatHoldService;
    private final BookingExpirationService bookingExpirationService;
    private final PaymentExpirationService paymentExpirationService;

    @Scheduled(
            fixedDelayString =
                    "${booking.expiration-poll-interval-ms:2000}"
    )
    public void expireReservations() {
        int holds = showSeatHoldService.expireActiveHolds();
        int bookings = bookingExpirationService.expirePendingBookings();
        int payments = paymentExpirationService.expirePendingPayments();

        if (holds + bookings + payments > 0) {
            log.info(
                    "Expiration cycle completed: holds={}, bookings={}, payments={}",
                    holds,
                    bookings,
                    payments
            );
        }
    }
}
