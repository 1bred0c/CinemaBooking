package congtuong.dev.cinemabooking.messaging.event;

import congtuong.dev.cinemabooking.entity.Booking;
import congtuong.dev.cinemabooking.entity.Payment;
import congtuong.dev.cinemabooking.entity.enums.NotificationType;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

public record BookingNotificationEvent(
        UUID eventId,
        NotificationType type,
        UUID userId,
        String email,
        String fullName,
        UUID bookingId,
        UUID paymentId,
        String movieTitle,
        LocalDateTime showtimeStart,
        String reason,
        Instant occurredAt
) {
    public static BookingNotificationEvent confirmed(
            Booking booking,
            Payment payment,
            Instant occurredAt
    ) {
        return from(
                NotificationType.BOOKING_CONFIRMED,
                booking,
                payment,
                null,
                occurredAt
        );
    }

    public static BookingNotificationEvent expired(
            Booking booking,
            Instant occurredAt
    ) {
        return from(
                NotificationType.BOOKING_EXPIRED,
                booking,
                null,
                null,
                occurredAt
        );
    }

    public static BookingNotificationEvent paymentFailed(
            Booking booking,
            Payment payment,
            String reason,
            Instant occurredAt
    ) {
        return from(
                NotificationType.PAYMENT_FAILED,
                booking,
                payment,
                reason,
                occurredAt
        );
    }

    private static BookingNotificationEvent from(
            NotificationType type,
            Booking booking,
            Payment payment,
            String reason,
            Instant occurredAt
    ) {
        return new BookingNotificationEvent(
                UUID.randomUUID(),
                type,
                booking.getUser().getId(),
                booking.getUser().getEmail(),
                booking.getUser().getFullname(),
                booking.getId(),
                payment == null ? null : payment.getId(),
                booking.getShowtime().getMovie().getTitle(),
                booking.getShowtime().getStartTime(),
                reason,
                occurredAt
        );
    }
}
