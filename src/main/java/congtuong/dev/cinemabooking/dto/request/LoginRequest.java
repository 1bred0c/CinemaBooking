package congtuong.dev.cinemabooking.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank
        String phoneNumber,

        @NotBlank
        String password
) {
}
