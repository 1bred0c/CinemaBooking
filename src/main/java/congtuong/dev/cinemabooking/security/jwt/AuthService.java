package congtuong.dev.cinemabooking.security.jwt;

import congtuong.dev.cinemabooking.dto.request.LoginRequest;
import congtuong.dev.cinemabooking.dto.request.LogoutRequest;
import congtuong.dev.cinemabooking.dto.request.RefreshTokenRequest;
import congtuong.dev.cinemabooking.dto.request.RegisterRequest;
import congtuong.dev.cinemabooking.dto.response.LoginResponse;
import congtuong.dev.cinemabooking.dto.response.RefreshTokenResponse;
import congtuong.dev.cinemabooking.dto.response.UserRespone;
import congtuong.dev.cinemabooking.dto.response.MyProfileResponse;

import java.util.UUID;

public interface AuthService {
    UserRespone register (RegisterRequest registerRequest);
    LoginResponse login (LoginRequest loginRequest);
    RefreshTokenResponse refreshToken (RefreshTokenRequest refreshTokenRequest);
    void logout (LogoutRequest logoutRequest);
    MyProfileResponse getMyProfile(UUID currentUserId);
}
