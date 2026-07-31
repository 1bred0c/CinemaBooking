package congtuong.dev.cinemabooking.dto.response;

import congtuong.dev.cinemabooking.entity.enums.Role;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record MyProfileResponse(
        UUID id,
        String phoneNumber,
        String email,
        String fullname,
        LocalDate birthDate,
        Role role,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
