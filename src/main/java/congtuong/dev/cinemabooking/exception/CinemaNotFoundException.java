package congtuong.dev.cinemabooking.exception;

import org.springframework.http.HttpStatus;

public class CinemaNotFoundException extends BusinessException {
    public CinemaNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND,
                message);
    }
}
