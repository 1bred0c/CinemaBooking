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

    @Override
    public void sendPasswordResetEmail(String recipient, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(recipient);
        message.setSubject("Reset your CinemaBooking password");
        message.setText("""
                Use the token below to reset your password. It expires in 15 minutes.

                %s

                If you did not request this change, ignore this email.
                """.formatted(token));
        mailSender.send(message);
    }
}
