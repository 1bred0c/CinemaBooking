package congtuong.dev.cinemabooking.notification;

import congtuong.dev.cinemabooking.config.MailProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WelcomeEmailServiceImpl implements WelcomeEmailService {

    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;

    @Override
    public void sendWelcomeEmail(String recipient, String fullName) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(recipient);
        message.setSubject("Welcome to CinemaBooking");
        message.setText("""
                Hello %s,

                Welcome to CinemaBooking! Your account has been created successfully.

                We hope you enjoy booking your next movie with us.

                CinemaBooking Team
                """.formatted(fullName));

        mailSender.send(message);
    }
}
