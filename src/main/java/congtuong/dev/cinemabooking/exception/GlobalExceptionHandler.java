package congtuong.dev.cinemabooking.exception;

import congtuong.dev.cinemabooking.dto.response.ErrorRespone;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
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


}
