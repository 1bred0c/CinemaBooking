package congtuong.dev.cinemabooking;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "booking.expiration-enabled=false",
        "spring.ai.openai.api-key=test-key"
})
class CinemaBookingApplicationTests {

    @Test
    void contextLoads() {
    }

}
