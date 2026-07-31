package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.BookingCreateRequest;
import congtuong.dev.cinemabooking.dto.response.BookingResponse;
import congtuong.dev.cinemabooking.dto.response.BookingSummaryResponse;
import congtuong.dev.cinemabooking.dto.response.MyBookingResponse;
import congtuong.dev.cinemabooking.entity.enums.BookingStatus;

import java.util.List;
import java.util.UUID;

public interface BookingService {

    BookingResponse createBooking(
            UUID currentUserId,
            BookingCreateRequest request
    );

    BookingResponse getBooking(UUID currentUserId, UUID bookingId);

    List<BookingSummaryResponse> getUserBookings(UUID currentUserId);

    List<MyBookingResponse> getMyBookings(
            UUID currentUserId,
            BookingStatus status
    );

    BookingResponse cancelBooking(UUID currentUserId, UUID bookingId);
}
