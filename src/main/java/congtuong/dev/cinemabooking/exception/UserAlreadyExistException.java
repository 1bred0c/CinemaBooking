package congtuong.dev.cinemabooking.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistException extends BusinessException {
    public UserAlreadyExistException(String message) {
        super(
                HttpStatus.CONFLICT,
                "User is already exist"
        );
    }
}
