package congtuong.dev.cinemabooking.exception;

import org.springframework.http.HttpStatus;

public class RoomException extends BusinessException {
    public RoomException(String message) {
        super(
                HttpStatus.BAD_REQUEST,
                message);
    }
}
