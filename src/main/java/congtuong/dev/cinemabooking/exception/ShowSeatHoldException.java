package congtuong.dev.cinemabooking.exception;

import org.springframework.http.HttpStatus;

public class ShowSeatHoldException extends BusinessException {
    public ShowSeatHoldException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public ShowSeatHoldException(HttpStatus status, String message) {
        super(status, message);
    }
}
