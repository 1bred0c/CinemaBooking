package congtuong.dev.cinemabooking.security.jwt;

import congtuong.dev.cinemabooking.entity.enums.Role;
import congtuong.dev.cinemabooking.entity.User;
import congtuong.dev.cinemabooking.entity.Booking;
import io.jsonwebtoken.Claims;

import java.time.Instant;
import java.util.UUID;

public interface JwtService {
    String generateAccessToken(User user);

    Claims parseToken(String token);

    UUID extractUserId(String token);

    String extractJti(String token);

    Instant extractExpiration(String token);

    Role extractRole(String token);

    long extractSecurityVersion(String token);
    String generateTicketToken(Booking booking);
    UUID extractTicketBookingId(String token);
}
