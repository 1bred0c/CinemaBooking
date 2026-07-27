package congtuong.dev.cinemabooking.exception;

import org.springframework.http.HttpStatus;

public class ShowtimeException extends BusinessException {
    public ShowtimeException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
