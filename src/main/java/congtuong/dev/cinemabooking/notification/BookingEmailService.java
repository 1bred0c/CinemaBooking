package congtuong.dev.cinemabooking.notification;

import congtuong.dev.cinemabooking.messaging.event.BookingNotificationEvent;

public interface BookingEmailService {

    void sendBookingConfirmation(BookingNotificationEvent event);
}
