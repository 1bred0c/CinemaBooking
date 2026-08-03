package congtuong.dev.cinemabooking.controller;

import congtuong.dev.cinemabooking.dto.request.BookingCreateRequest;
import congtuong.dev.cinemabooking.dto.response.BookingResponse;
import congtuong.dev.cinemabooking.dto.response.BookingSummaryResponse;
import congtuong.dev.cinemabooking.dto.response.MyBookingResponse;
import congtuong.dev.cinemabooking.entity.enums.BookingStatus;
import congtuong.dev.cinemabooking.security.jwt.CustomUserDetails;
import congtuong.dev.cinemabooking.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
@RequiredArgsConstructor
public class BookingController {

    private final BookingService bookingService;

    @PostMapping
    public ResponseEntity<BookingResponse> createBooking(
            @Valid @RequestBody BookingCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        BookingResponse response = bookingService.createBooking(
                currentUser.getUser().getId(),
                request
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{bookingId}")
    public ResponseEntity<BookingResponse> getBooking(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                bookingService.getBooking(
                        currentUser.getUser().getId(),
                        bookingId
                )
        );
    }

    @GetMapping("/by-hold/{holdId}")
    public ResponseEntity<BookingResponse> getBookingByHold(
            @PathVariable UUID holdId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(bookingService.getBookingByHold(
                currentUser.getUser().getId(),
                holdId
        ));
    }

    @GetMapping("/current")
    public ResponseEntity<BookingResponse> getCurrentCheckoutBooking(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                bookingService.getCurrentCheckoutBooking(
                        currentUser.getUser().getId()
                )
        );
    }

    @GetMapping
    public ResponseEntity<Page<BookingSummaryResponse>> getUserBookings(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(
                bookingService.getUserBookings(
                        currentUser.getUser().getId(),
                        pageable
                )
        );
    }

    @GetMapping("/me")
    public ResponseEntity<Page<MyBookingResponse>> getMyBookings(
            @RequestParam(required = false) BookingStatus status,
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(bookingService.getMyBookings(
                currentUser.getUser().getId(),
                status,
                pageable
        ));
    }

    @PatchMapping("/{bookingId}/cancel")
    public ResponseEntity<BookingResponse> cancelBooking(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(
                bookingService.cancelBooking(
                        currentUser.getUser().getId(),
                        bookingId
                )
        );
    }
}
