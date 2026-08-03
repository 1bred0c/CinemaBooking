package congtuong.dev.cinemabooking.controller;

import congtuong.dev.cinemabooking.dto.request.LoginRequest;
import congtuong.dev.cinemabooking.dto.request.LogoutRequest;
import congtuong.dev.cinemabooking.dto.request.RefreshTokenRequest;
import congtuong.dev.cinemabooking.dto.request.RegisterRequest;
import congtuong.dev.cinemabooking.dto.request.ChangePasswordRequest;
import congtuong.dev.cinemabooking.dto.response.LoginResponse;
import congtuong.dev.cinemabooking.dto.response.RefreshTokenResponse;
import congtuong.dev.cinemabooking.dto.response.UserRespone;
import congtuong.dev.cinemabooking.dto.response.MyProfileResponse;
import congtuong.dev.cinemabooking.security.jwt.AuthService;
import congtuong.dev.cinemabooking.security.jwt.CustomUserDetails;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<UserRespone> register (@Valid @RequestBody RegisterRequest request){
        UserRespone userRespone = authService.register(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userRespone);
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<RefreshTokenResponse> refreshToken (@Valid @RequestBody RefreshTokenRequest request){
        RefreshTokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout (@Valid @RequestBody LogoutRequest request){
        authService.logout(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login (@Valid @RequestBody LoginRequest request){
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my-profile")
    public ResponseEntity<MyProfileResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(authService.getMyProfile(
                currentUser.getUser().getId()
        ));
    }

    @PatchMapping("/change-password")
    public ResponseEntity<Void> changePassword(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        authService.changePassword(currentUser.getUser().getId(), request);
        return ResponseEntity.noContent().build();
    }

}
