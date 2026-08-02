package congtuong.dev.cinemabooking.exception;

import org.springframework.http.HttpStatus;

public class NotificationException extends BusinessException {

    public NotificationException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
