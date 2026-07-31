package congtuong.dev.cinemabooking.service.expiration;

import congtuong.dev.cinemabooking.entity.Booking;
import congtuong.dev.cinemabooking.entity.BookingItem;
import congtuong.dev.cinemabooking.entity.ShowSeat;
import congtuong.dev.cinemabooking.entity.ShowSeatHold;
import congtuong.dev.cinemabooking.entity.enums.BookingStatus;
import congtuong.dev.cinemabooking.entity.enums.ShowSeatHoldStatus;
import congtuong.dev.cinemabooking.entity.enums.ShowSeatStatus;
import congtuong.dev.cinemabooking.repository.BookingItemRepository;
import congtuong.dev.cinemabooking.repository.BookingRepository;
import congtuong.dev.cinemabooking.repository.ShowSeatHoldRepository;
import congtuong.dev.cinemabooking.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingExpirationWorker {

    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final ShowSeatHoldRepository showSeatHoldRepository;
    private final ShowSeatRepository showSeatRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean expire(UUID bookingId, Instant now) {
        Booking booking = bookingRepository
                .findByIdForUpdate(bookingId)
                .orElse(null);

        if (booking == null
                || booking.getStatus() != BookingStatus.PENDING_PAYMENT
                || booking.getPaymentExpiresAt().isAfter(now)) {
            return false;
        }

        ShowSeatHold hold = showSeatHoldRepository
                .findByIdForUpdate(booking.getHold().getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Booking hold no longer exists"
                ));

        List<UUID> showSeatIds = bookingItemRepository
                .findAllByBookingId(bookingId)
                .stream()
                .map(BookingItem::getShowSeat)
                .map(ShowSeat::getId)
                .sorted()
                .toList();
        List<ShowSeat> lockedSeats =
                showSeatRepository.findAllByIdForUpdate(showSeatIds);
        if (showSeatIds.isEmpty()
                || lockedSeats.size() != showSeatIds.size()) {
            throw new IllegalStateException(
                    "Expiring booking has missing seat records"
            );
        }

        lockedSeats.stream()
                .filter(seat -> seat.getStatus() == ShowSeatStatus.HELD)
                .forEach(seat ->
                        seat.setStatus(ShowSeatStatus.AVAILABLE)
                );

        if (hold.getStatus() == ShowSeatHoldStatus.CONFIRMED) {
            hold.setStatus(ShowSeatHoldStatus.EXPIRED);
        }
        booking.markExpired();
        return true;
    }
}
