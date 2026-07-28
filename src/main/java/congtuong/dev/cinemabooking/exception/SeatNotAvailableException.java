package congtuong.dev.cinemabooking.exception;

import org.springframework.http.HttpStatus;

public class SeatNotAvailableException extends BusinessException {
    public SeatNotAvailableException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
