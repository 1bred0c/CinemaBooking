package congtuong.dev.cinemabooking.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "booking")
public record BookingProperties(
        long seatHoldDurationMinutes
) {
}
