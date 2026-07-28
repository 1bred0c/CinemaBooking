package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.BookingCreateRequest;
import congtuong.dev.cinemabooking.dto.response.BookingResponse;
import congtuong.dev.cinemabooking.dto.response.BookingSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface BookingService {

    BookingResponse createBooking(
            UUID currentUserId,
            BookingCreateRequest request
    );

    BookingResponse getBooking(UUID currentUserId, UUID bookingId);

    List<BookingSummaryResponse> getUserBookings(UUID currentUserId);

    BookingResponse cancelBooking(UUID currentUserId, UUID bookingId);
}
