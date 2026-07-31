package congtuong.dev.cinemabooking.exception;

import org.springframework.http.HttpStatus;

public class PaymentException extends BusinessException {

    public PaymentException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }

    public PaymentException(HttpStatus status, String message) {
        super(status, message);
    }
}
