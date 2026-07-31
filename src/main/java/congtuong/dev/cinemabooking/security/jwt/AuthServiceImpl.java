package congtuong.dev.cinemabooking.security.jwt;

import congtuong.dev.cinemabooking.dto.request.LoginRequest;
import congtuong.dev.cinemabooking.dto.request.LogoutRequest;
import congtuong.dev.cinemabooking.dto.request.RefreshTokenRequest;
import congtuong.dev.cinemabooking.dto.request.RegisterRequest;
import congtuong.dev.cinemabooking.dto.response.LoginResponse;
import congtuong.dev.cinemabooking.dto.response.RefreshTokenResponse;
import congtuong.dev.cinemabooking.dto.response.UserRespone;
import congtuong.dev.cinemabooking.dto.response.MyProfileResponse;
import congtuong.dev.cinemabooking.entity.RefreshToken;
import congtuong.dev.cinemabooking.entity.enums.Role;
import congtuong.dev.cinemabooking.entity.User;
import congtuong.dev.cinemabooking.exception.UserAlreadyExistException;
import congtuong.dev.cinemabooking.exception.UserNotFoundException;
import congtuong.dev.cinemabooking.messaging.event.UserRegisteredEvent;
import congtuong.dev.cinemabooking.repository.UserRepository;
import congtuong.dev.cinemabooking.service.OutboxEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final OutboxEventService outboxEventService;

    @Override
    @Transactional
    public UserRespone register(RegisterRequest registerRequest) {
        String phoneNumber = registerRequest.phoneNumber();
        String email = registerRequest.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByPhoneNumber(phoneNumber))
            throw new UserAlreadyExistException("Phone number is already exist");
        if (userRepository.existsByEmailIgnoreCase(email))
            throw new UserAlreadyExistException("Email already exists");
        User newUser = User.builder()
                .phoneNumber(phoneNumber)
                .email(email)
                .fullname(registerRequest.fullname())
                .password(passwordEncoder.encode(registerRequest.password()))
                .birthDate(registerRequest.birthdate())
                .isActive(true)
                .role(Role.CUSTOMER)
                .build();

        try {
            User savedUser = userRepository.save(newUser);
            outboxEventService.append(
                    UserRegisteredEvent.from(savedUser)
            );

            return new UserRespone(
                    savedUser.getId(),
                    savedUser.getPhoneNumber(),
                    savedUser.getEmail(),
                    savedUser.getFullname(),
                    savedUser.getRole()
            );
        } catch (DataIntegrityViolationException exception){
            throw new UserAlreadyExistException("Phone number or email already exists");
        }
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        UsernamePasswordAuthenticationToken authenticationToken =
                new UsernamePasswordAuthenticationToken(
                        loginRequest.phoneNumber(),
                        loginRequest.password()
                );
        Authentication authentication = authenticationManager.authenticate(authenticationToken);
        //Try to write down myself
        CustomUserDetails customUserDetails = (CustomUserDetails) authentication.getPrincipal();
        String accessToken = jwtService.generateAccessToken(customUserDetails.getUser());
        return new  LoginResponse(
                accessToken,
                refreshTokenService.generateRefreshToken(customUserDetails.getUser())
        );
    }

    @Override
    public RefreshTokenResponse refreshToken(RefreshTokenRequest refreshTokenRequest) {
        RefreshToken token = refreshTokenService.getValidRefreshToken(refreshTokenRequest.refreshToken());
        String newAccessToken = jwtService.generateAccessToken(token.getUser());
        return new RefreshTokenResponse(newAccessToken, refreshTokenRequest.refreshToken());
    }

    @Override
    public void logout(LogoutRequest logoutRequest) {
        refreshTokenService.revokeRefreshToken(logoutRequest.refreshToken());
    }

    @Override
    @Transactional(readOnly = true)
    public MyProfileResponse getMyProfile(UUID currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() ->
                        new UserNotFoundException("User not found")
                );
        return new MyProfileResponse(
                user.getId(),
                user.getPhoneNumber(),
                user.getEmail(),
                user.getFullname(),
                user.getBirthDate(),
                user.getRole(),
                user.isActive(),
                user.getCreateAt().toLocalDateTime(),
                user.getUpdateAt().toLocalDateTime()
        );
    }
}
