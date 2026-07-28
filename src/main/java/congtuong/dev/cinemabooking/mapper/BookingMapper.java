package congtuong.dev.cinemabooking.mapper;

import congtuong.dev.cinemabooking.dto.response.BookingItemResponse;
import congtuong.dev.cinemabooking.dto.response.BookingResponse;
import congtuong.dev.cinemabooking.dto.response.BookingSummaryResponse;
import congtuong.dev.cinemabooking.entity.Booking;
import congtuong.dev.cinemabooking.entity.BookingItem;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookingMapper {

    public BookingResponse toResponse(
            Booking booking,
            List<BookingItem> bookingItems
    ) {
        return new BookingResponse(
                booking.getId(),
                booking.getUser().getId(),
                booking.getShowtime().getId(),
                booking.getHold().getId(),
                booking.getStatus(),
                booking.getTotalAmount(),
                booking.getPaymentExpiresAt(),
                booking.getConfirmedAt(),
                booking.getCancelledAt(),
                booking.getCreatedAt(),
                bookingItems.stream()
                        .map(this::toItemResponse)
                        .toList()
        );
    }

    public BookingSummaryResponse toSummaryResponse(Booking booking) {
        return new BookingSummaryResponse(
                booking.getId(),
                booking.getShowtime().getId(),
                booking.getStatus(),
                booking.getTotalAmount(),
                booking.getPaymentExpiresAt(),
                booking.getCreatedAt()
        );
    }

    public BookingItemResponse toItemResponse(BookingItem bookingItem) {
        return new BookingItemResponse(
                bookingItem.getId(),
                bookingItem.getShowSeat().getId(),
                bookingItem.getSeatRow(),
                bookingItem.getSeatNumber(),
                bookingItem.getSeatType(),
                bookingItem.getUnitPrice()
        );
    }
}
