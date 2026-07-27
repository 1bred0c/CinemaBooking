package congtuong.dev.cinemabooking.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken
) {
}
