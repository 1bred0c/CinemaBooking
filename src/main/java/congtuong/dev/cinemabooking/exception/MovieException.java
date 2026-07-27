package congtuong.dev.cinemabooking.exception;

import org.springframework.http.HttpStatus;

public class MovieException extends BusinessException {
    public MovieException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
