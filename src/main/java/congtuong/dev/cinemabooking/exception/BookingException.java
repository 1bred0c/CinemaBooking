package congtuong.dev.cinemabooking.exception;

import org.springframework.http.HttpStatus;

public class BookingException extends BusinessException {

    public BookingException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public BookingException(HttpStatus status, String message) {
        super(status, message);
    }
}
