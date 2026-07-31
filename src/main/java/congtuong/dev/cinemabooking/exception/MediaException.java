package congtuong.dev.cinemabooking.exception;

import org.springframework.http.HttpStatus;

public class MediaException extends BusinessException {

    public MediaException(HttpStatus status, String message) {
        super(status, message);
    }
}
