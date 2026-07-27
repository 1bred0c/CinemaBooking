package congtuong.dev.cinemabooking.exception;

import org.springframework.http.HttpStatus;

public class GenreException extends BusinessException {
    public GenreException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
