package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.BookingCreateRequest;
import congtuong.dev.cinemabooking.dto.response.BookingResponse;
import congtuong.dev.cinemabooking.dto.response.BookingSummaryResponse;
import congtuong.dev.cinemabooking.dto.response.MyBookingResponse;
import congtuong.dev.cinemabooking.entity.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface BookingService {

    BookingResponse createBooking(
            UUID currentUserId,
            BookingCreateRequest request
    );

    BookingResponse getBooking(UUID currentUserId, UUID bookingId);

    Page<BookingSummaryResponse> getUserBookings(
            UUID currentUserId,
            Pageable pageable
    );

    Page<MyBookingResponse> getMyBookings(
            UUID currentUserId,
            BookingStatus status,
            Pageable pageable
    );

    BookingResponse cancelBooking(UUID currentUserId, UUID bookingId);
}
