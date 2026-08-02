package congtuong.dev.cinemabooking.notification;

import congtuong.dev.cinemabooking.config.MailProperties;
import congtuong.dev.cinemabooking.messaging.event.BookingNotificationEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BookingEmailServiceImpl implements BookingEmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    @Override
    public void sendBookingConfirmation(BookingNotificationEvent event) {
        if (event.email() == null || event.email().isBlank()) {
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(event.email());
        message.setSubject("CinemaBooking - Booking confirmed");
        message.setText("""
                Hello %s,

                Your booking has been confirmed successfully.

                Movie: %s
                Showtime: %s
                Booking ID: %s

                CinemaBooking Team
                """.formatted(
                event.fullName(),
                event.movieTitle(),
                event.showtimeStart(),
                event.bookingId()
        ));
        mailSender.send(message);
    }
}
