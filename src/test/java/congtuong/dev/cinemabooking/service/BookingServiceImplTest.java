package congtuong.dev.cinemabooking.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Skeleton tests for the intentionally unimplemented booking workflow")
class BookingServiceImplTest {

    @Test
    void createBooking_shouldCreatePendingBooking_whenHoldIsValid() {
        // TODO BOOKING TEST: Implement after booking core logic is completed.
    }

    @Test
    void createBooking_shouldRejectExpiredHold() {
        // TODO BOOKING TEST: Implement after booking core logic is completed.
    }

    @Test
    void createBooking_shouldRejectHoldOwnedByAnotherUser() {
        // TODO BOOKING TEST: Implement after booking core logic is completed.
    }

    @Test
    void createBooking_shouldCreateOnlyOneBookingForSameHold() {
        // TODO BOOKING TEST: Implement after booking core logic is completed.
    }

    @Test
    void createBooking_shouldRejectSeatStateConflict() {
        // TODO BOOKING TEST: Implement after booking core logic is completed.
    }

    @Test
    void cancelBooking_shouldReleasePendingBookingResources() {
        // TODO BOOKING TEST: Implement after cancellation logic is completed.
    }
}
