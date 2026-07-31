package congtuong.dev.cinemabooking.mapper;

import congtuong.dev.cinemabooking.dto.response.BookingItemResponse;
import congtuong.dev.cinemabooking.dto.response.BookingResponse;
import congtuong.dev.cinemabooking.dto.response.BookingSummaryResponse;
import congtuong.dev.cinemabooking.entity.Booking;
import congtuong.dev.cinemabooking.entity.BookingItem;
import congtuong.dev.cinemabooking.entity.ShowTime;
import congtuong.dev.cinemabooking.entity.Movie;
import congtuong.dev.cinemabooking.entity.Room;
import congtuong.dev.cinemabooking.entity.Cinema;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BookingMapper {

    public BookingResponse toResponse(
            Booking booking,
            List<BookingItem> bookingItems
    ) {
        ShowTime showtime = booking.getShowtime();
        Movie movie = showtime.getMovie();
        Room room = showtime.getRoom();
        Cinema cinema = room.getCinema();
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
                movie.getId(),
                movie.getTitle(),
                movie.getPosterUrl(),
                cinema.getId(),
                cinema.getName(),
                cinema.getAddress(),
                room.getId(),
                room.getName(),
                showtime.getStartTime(),
                showtime.getEndTime(),
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
