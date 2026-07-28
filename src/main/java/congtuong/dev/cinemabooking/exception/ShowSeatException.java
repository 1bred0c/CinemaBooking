package congtuong.dev.cinemabooking.exception;

import org.springframework.http.HttpStatus;

public class ShowSeatException extends BusinessException {
    public ShowSeatException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public ShowSeatException(HttpStatus status, String message) {
        super(status, message);
    }
}
