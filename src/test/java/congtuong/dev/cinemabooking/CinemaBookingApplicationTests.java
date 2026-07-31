package congtuong.dev.cinemabooking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "booking.expiration-enabled=false")
class CinemaBookingApplicationTests {

    @Test
    void contextLoads() {
    }

}
