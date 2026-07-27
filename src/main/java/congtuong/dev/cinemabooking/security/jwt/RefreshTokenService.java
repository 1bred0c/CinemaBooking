package congtuong.dev.cinemabooking.security.jwt;

import congtuong.dev.cinemabooking.entity.RefreshToken;
import congtuong.dev.cinemabooking.entity.User;

public interface RefreshTokenService {
    String generateRefreshToken(User user);
    RefreshToken getValidRefreshToken(String refreshToken);
    void revokeRefreshToken(String refreshToken);
}
