package congtuong.dev.cinemabooking.exception;

import congtuong.dev.cinemabooking.dto.response.ErrorRespone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorRespone> handleBusinessException(BusinessException ex) {
        ErrorRespone response = new ErrorRespone(
                ex.getStatus().value(),
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(ex.getStatus())
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespone> handleValidationException(
            MethodArgumentNotValidException exception
    ) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(fieldError -> fieldError.getDefaultMessage())
                .orElse("Request validation failed");

        return errorResponse(HttpStatus.BAD_REQUEST, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespone> handleUnreadableMessage(
            HttpMessageNotReadableException exception
    ) {
        return errorResponse(
                HttpStatus.BAD_REQUEST,
                "Request body is missing or malformed"
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespone> handleUnexpectedException(
            Exception exception
    ) {
        log.error("Unexpected request failure", exception);
        return errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error"
        );
    }

    private ResponseEntity<ErrorRespone> errorResponse(
            HttpStatus status,
            String message
    ) {
        ErrorRespone response = new ErrorRespone(
                status.value(),
                message,
                LocalDateTime.now()
        );
        return ResponseEntity.status(status).body(response);
    }


}
