package congtuong.dev.cinemabooking.repository;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled("Skeleton tests for the intentionally unimplemented booking workflow")
class BookingIntegrationTest {

    @Test
    void bookingRepository_shouldEnforceOneBookingPerHold() {
        // TODO BOOKING TEST: Implement with PostgreSQL Testcontainers.
    }

    @Test
    void concurrentCreateBooking_shouldCreateOnlyOneBookingForSameHold() {
        // TODO BOOKING TEST: Use PostgreSQL Testcontainers, concurrent threads,
        // and CountDownLatch rather than Thread.sleep for synchronization.
    }
}
