package congtuong.dev.cinemabooking.dto.response;

import congtuong.dev.cinemabooking.entity.enums.Role;

import java.util.UUID;

public record UserRespone(
        UUID id,
        String phoneNumber,
        String email,
        String fullname,
        Role role) {
}
