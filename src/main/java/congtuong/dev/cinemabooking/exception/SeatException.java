package congtuong.dev.cinemabooking.exception;

import org.springframework.http.HttpStatus;

public class SeatException extends BusinessException {
    public SeatException(String message) {
        super(
                HttpStatus.BAD_REQUEST,
                message);
    }
}
