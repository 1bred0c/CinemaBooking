package congtuong.dev.cinemabooking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProfileRequest(
        @Size(min = 2, max = 100) String fullname,
        @Email String email,
        @Past LocalDate birthDate
) {
}
