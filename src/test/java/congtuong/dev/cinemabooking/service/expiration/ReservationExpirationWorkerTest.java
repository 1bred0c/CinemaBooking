package congtuong.dev.cinemabooking.service.expiration;

import congtuong.dev.cinemabooking.entity.Booking;
import congtuong.dev.cinemabooking.entity.BookingItem;
import congtuong.dev.cinemabooking.entity.Payment;
import congtuong.dev.cinemabooking.entity.ShowSeat;
import congtuong.dev.cinemabooking.entity.ShowSeatHold;
import congtuong.dev.cinemabooking.entity.ShowSeatHoldItem;
import congtuong.dev.cinemabooking.entity.ShowTime;
import congtuong.dev.cinemabooking.entity.Movie;
import congtuong.dev.cinemabooking.entity.User;
import congtuong.dev.cinemabooking.entity.enums.BookingStatus;
import congtuong.dev.cinemabooking.entity.enums.PaymentStatus;
import congtuong.dev.cinemabooking.entity.enums.ShowSeatHoldStatus;
import congtuong.dev.cinemabooking.entity.enums.ShowSeatStatus;
import congtuong.dev.cinemabooking.repository.BookingItemRepository;
import congtuong.dev.cinemabooking.repository.BookingRepository;
import congtuong.dev.cinemabooking.repository.PaymentRepository;
import congtuong.dev.cinemabooking.repository.ShowSeatHoldItemRepository;
import congtuong.dev.cinemabooking.repository.ShowSeatHoldRepository;
import congtuong.dev.cinemabooking.repository.ShowSeatRepository;
import congtuong.dev.cinemabooking.service.OutboxEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservationExpirationWorkerTest {

    @Mock
    private BookingRepository bookingRepository;
    @Mock
    private BookingItemRepository bookingItemRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private ShowSeatHoldRepository showSeatHoldRepository;
    @Mock
    private ShowSeatHoldItemRepository showSeatHoldItemRepository;
    @Mock
    private ShowSeatRepository showSeatRepository;
    @Mock
    private OutboxEventService outboxEventService;

    @Test
    void expiredActiveHoldReleasesHeldSeats() {
        Instant now = Instant.now();
        UUID holdId = UUID.randomUUID();
        ShowSeatHold hold = ShowSeatHold.builder()
                .id(holdId)
                .status(ShowSeatHoldStatus.ACTIVE)
                .expiresAt(now.minusSeconds(1))
                .build();
        ShowSeat seat = ShowSeat.builder()
                .id(UUID.randomUUID())
                .status(ShowSeatStatus.HELD)
                .build();
        ShowSeatHoldItem item = ShowSeatHoldItem.builder()
                .showSeatHold(hold)
                .showSeat(seat)
                .build();
        when(showSeatHoldRepository.findByIdForUpdate(holdId))
                .thenReturn(Optional.of(hold));
        when(showSeatHoldItemRepository.findAllByShowSeatHoldId(holdId))
                .thenReturn(List.of(item));
        when(showSeatRepository.findAllByIdForUpdate(List.of(seat.getId())))
                .thenReturn(List.of(seat));
        ShowSeatHoldExpirationWorker worker =
                new ShowSeatHoldExpirationWorker(
                        showSeatHoldRepository,
                        showSeatHoldItemRepository,
                        showSeatRepository
                );

        boolean expired = worker.expire(holdId, now);

        assertThat(expired).isTrue();
        assertThat(hold.getStatus())
                .isEqualTo(ShowSeatHoldStatus.EXPIRED);
        assertThat(seat.getStatus()).isEqualTo(ShowSeatStatus.AVAILABLE);
    }

    @Test
    void expiredPendingBookingReleasesSeatsAndExpiresHold() {
        Instant now = Instant.now();
        UUID bookingId = UUID.randomUUID();
        ShowSeatHold hold = ShowSeatHold.builder()
                .id(UUID.randomUUID())
                .status(ShowSeatHoldStatus.CONFIRMED)
                .expiresAt(now.minusSeconds(1))
                .build();
        Booking booking = Booking.builder()
                .id(bookingId)
                .hold(hold)
                .user(User.builder()
                        .id(UUID.randomUUID())
                        .email("user@example.com")
                        .fullname("Cinema User")
                        .build())
                .showtime(ShowTime.builder()
                        .movie(Movie.builder().title("Test Movie").build())
                        .build())
                .status(BookingStatus.PENDING_PAYMENT)
                .paymentExpiresAt(now.minusSeconds(1))
                .build();
        ShowSeat seat = ShowSeat.builder()
                .id(UUID.randomUUID())
                .status(ShowSeatStatus.HELD)
                .build();
        BookingItem item = BookingItem.builder()
                .booking(booking)
                .showSeat(seat)
                .build();
        when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));
        when(showSeatHoldRepository.findByIdForUpdate(hold.getId()))
                .thenReturn(Optional.of(hold));
        when(bookingItemRepository.findAllByBookingId(bookingId))
                .thenReturn(List.of(item));
        when(showSeatRepository.findAllByIdForUpdate(List.of(seat.getId())))
                .thenReturn(List.of(seat));
        BookingExpirationWorker worker = new BookingExpirationWorker(
                bookingRepository,
                bookingItemRepository,
                showSeatHoldRepository,
                showSeatRepository,
                outboxEventService
        );

        boolean expired = worker.expire(bookingId, now);

        assertThat(expired).isTrue();
        assertThat(booking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(hold.getStatus())
                .isEqualTo(ShowSeatHoldStatus.EXPIRED);
        assertThat(seat.getStatus()).isEqualTo(ShowSeatStatus.AVAILABLE);
    }

    @Test
    void confirmedBookingIsNotExpiredAfterCandidateSelection() {
        Instant now = Instant.now();
        UUID bookingId = UUID.randomUUID();
        Booking booking = Booking.builder()
                .id(bookingId)
                .status(BookingStatus.CONFIRMED)
                .paymentExpiresAt(now.minusSeconds(1))
                .build();
        when(bookingRepository.findByIdForUpdate(bookingId))
                .thenReturn(Optional.of(booking));
        BookingExpirationWorker worker = new BookingExpirationWorker(
                bookingRepository,
                bookingItemRepository,
                showSeatHoldRepository,
                showSeatRepository,
                outboxEventService
        );

        boolean expired = worker.expire(bookingId, now);

        assertThat(expired).isFalse();
        verify(showSeatHoldRepository, never()).findByIdForUpdate(
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void pendingPaymentPastDeadlineBecomesExpired() {
        Instant now = Instant.now();
        UUID paymentId = UUID.randomUUID();
        Payment payment = Payment.builder()
                .id(paymentId)
                .status(PaymentStatus.PENDING)
                .expiresAt(now.minusSeconds(1))
                .build();
        when(paymentRepository.findByIdForUpdate(paymentId))
                .thenReturn(Optional.of(payment));
        PaymentExpirationWorker worker =
                new PaymentExpirationWorker(paymentRepository);

        boolean expired = worker.expire(paymentId, now);

        assertThat(expired).isTrue();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.EXPIRED);
        assertThat(payment.getFailureReason())
                .isEqualTo("Payment period expired");
        assertThat(payment.getFailedAt()).isEqualTo(now);
    }
}
