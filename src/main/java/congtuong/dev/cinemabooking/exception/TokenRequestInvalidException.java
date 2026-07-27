package congtuong.dev.cinemabooking.exception;

import org.springframework.http.HttpStatus;

public class TokenRequestInvalidException extends BusinessException {
    public TokenRequestInvalidException(String message) {
        super(
                HttpStatus.UNAUTHORIZED,
                message);
    }
}
