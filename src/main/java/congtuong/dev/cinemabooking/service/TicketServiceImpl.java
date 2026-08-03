package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.response.TicketResponse;
import congtuong.dev.cinemabooking.entity.Booking;
import congtuong.dev.cinemabooking.entity.BookingItem;
import congtuong.dev.cinemabooking.entity.enums.BookingStatus;
import congtuong.dev.cinemabooking.exception.BookingException;
import congtuong.dev.cinemabooking.repository.BookingItemRepository;
import congtuong.dev.cinemabooking.repository.BookingRepository;
import congtuong.dev.cinemabooking.security.jwt.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketServiceImpl implements TicketService {
    private final BookingRepository bookingRepository;
    private final BookingItemRepository bookingItemRepository;
    private final JwtService jwtService;

    @Override
    @Transactional
    public TicketResponse getTicket(UUID currentUserId, UUID bookingId) {
        Booking booking = bookingRepository.findByIdAndUserId(bookingId, currentUserId)
                .orElseThrow(() -> new BookingException("Booking not found"));
        return ticket(booking);
    }

    @Override
    @Transactional
    public TicketResponse checkIn(String qrToken) {
        UUID bookingId = jwtService.extractTicketBookingId(qrToken);
        Booking booking = bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BookingException("Booking not found"));
        validateConfirmed(booking);
        Instant checkInDeadline = booking.getShowtime().getEndTime()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .plusSeconds(6 * 60 * 60);
        if (Instant.now().isAfter(checkInDeadline)) {
            throw new BookingException(HttpStatus.CONFLICT, "The ticket check-in window has closed");
        }
        if (booking.getCheckedInAt() == null) {
            booking.setCheckedInAt(Instant.now());
        }
        return ticket(booking);
    }

    private TicketResponse ticket(Booking booking) {
        validateConfirmed(booking);
        ensureBookingCode(booking);
        List<BookingItem> items = bookingItemRepository.findAllByBookingId(booking.getId());
        return new TicketResponse(
                booking.getId(),
                booking.getBookingCode(),
                booking.getShowtime().getMovie().getTitle(),
                booking.getShowtime().getRoom().getCinema().getName(),
                booking.getShowtime().getRoom().getName(),
                booking.getShowtime().getStartTime(),
                items.stream()
                        .map(item -> item.getSeatRow() + item.getSeatNumber())
                        .toList(),
                jwtService.generateTicketToken(booking),
                booking.getCheckedInAt()
        );
    }

    private void ensureBookingCode(Booking booking) {
        if (booking.getBookingCode() == null) {
            booking.setBookingCode("CB-" + booking.getId().toString()
                    .replace("-", "")
                    .substring(0, 12)
                    .toUpperCase());
        }
    }

    private void validateConfirmed(Booking booking) {
        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BookingException(HttpStatus.CONFLICT, "Only confirmed bookings have valid tickets");
        }
    }
}
