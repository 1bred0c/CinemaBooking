package congtuong.dev.cinemabooking.exception;

import org.springframework.http.HttpStatus;

public class UserSecurityException extends BusinessException {
    public UserSecurityException(HttpStatus status, String message) {
        super(status, message);
    }
}
