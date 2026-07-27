package congtuong.dev.cinemabooking.dto.response;

public record RefreshTokenResponse (
        String accessToken,
        String refreshToken
){
}
