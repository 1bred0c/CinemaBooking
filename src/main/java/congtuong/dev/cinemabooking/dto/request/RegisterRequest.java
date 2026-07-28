package congtuong.dev.cinemabooking.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;


public record RegisterRequest(
     @NotBlank(message = "Phone number is required")
     @Pattern(
             regexp = "^0\\d{9}$",
             message = "Phone number must contains 10 digits and start with 0"
     )
     String phoneNumber,

     @NotBlank(message = "Email is required")
     @Email(message = "Email must be valid")
     String email,

     @NotBlank(message = "Password is required")
     @Size(min = 6, max = 20, message = "Password must contains between 6 to 20 characters")
     String password,

     @NotBlank(message ="Full name is required")
     String fullname,

     @Past(message = "Birth date must be in the past")
     LocalDate birthdate
){
}
