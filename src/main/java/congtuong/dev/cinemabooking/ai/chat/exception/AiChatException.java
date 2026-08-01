package congtuong.dev.cinemabooking.ai.chat.exception;

import congtuong.dev.cinemabooking.exception.BusinessException;
import org.springframework.http.HttpStatus;

public class AiChatException extends BusinessException {

    public AiChatException(String message) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    public AiChatException(String message, Throwable cause) {
        super(HttpStatus.SERVICE_UNAVAILABLE, message);
        initCause(cause);
    }
}
