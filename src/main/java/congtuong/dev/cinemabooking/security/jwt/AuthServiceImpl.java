package congtuong.dev.cinemabooking.security.jwt;

import congtuong.dev.cinemabooking.dto.request.LoginRequest;
import congtuong.dev.cinemabooking.dto.request.LogoutRequest;
import congtuong.dev.cinemabooking.dto.request.RefreshTokenRequest;
import congtuong.dev.cinemabooking.dto.request.RegisterRequest;
import congtuong.dev.cinemabooking.dto.request.ChangePasswordRequest;
import congtuong.dev.cinemabooking.dto.request.ForgotPasswordRequest;
import congtuong.dev.cinemabooking.dto.request.ResetPasswordRequest;
import congtuong.dev.cinemabooking.dto.request.UpdateProfileRequest;
import congtuong.dev.cinemabooking.dto.response.LoginResponse;
import congtuong.dev.cinemabooking.dto.response.RefreshTokenResponse;
import congtuong.dev.cinemabooking.dto.response.UserRespone;
import congtuong.dev.cinemabooking.dto.response.MyProfileResponse;
import congtuong.dev.cinemabooking.entity.RefreshToken;
import congtuong.dev.cinemabooking.entity.enums.Role;
import congtuong.dev.cinemabooking.entity.User;
import congtuong.dev.cinemabooking.exception.UserAlreadyExistException;
import congtuong.dev.cinemabooking.exception.UserNotFoundException;
import congtuong.dev.cinemabooking.exception.UserSecurityException;
import congtuong.dev.cinemabooking.messaging.event.UserRegisteredEvent;
import congtuong.dev.cinemabooking.repository.UserRepository;
import congtuong.dev.cinemabooking.service.OutboxEventService;
import congtuong.dev.cinemabooking.notification.WelcomeEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.http.HttpStatus;

import java.util.Locale;
import java.util.UUID;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;
    private final OutboxEventService outboxEventService;
    private final SecurityStateService securityStateService;
    private final PasswordResetTokenService passwordResetTokenService;
    private final WelcomeEmailService welcomeEmailService;

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
        return toProfile(user);
    }

    @Override
    @Transactional
    public MyProfileResponse updateProfile(
            UUID currentUserId,
            UpdateProfileRequest request
    ) {
        User user = userRepository.findByIdForUpdate(currentUserId)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        if (request.fullname() != null) {
            user.setFullname(request.fullname().trim());
        }
        if (request.birthDate() != null) {
            user.setBirthDate(request.birthDate());
        }
        if (request.email() != null) {
            String email = request.email().trim().toLowerCase(Locale.ROOT);
            if (!Objects.equals(email, user.getEmail())
                    && userRepository.existsByEmailIgnoreCase(email)) {
                throw new UserAlreadyExistException("Email already exists");
            }
            user.setEmail(email);
        }
        return toProfile(user);
    }

    @Override
    @Transactional(readOnly = true)
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmailIgnoreCase(
                        request.email().trim().toLowerCase(Locale.ROOT)
                )
                .filter(User::isActive)
                .ifPresent(user -> welcomeEmailService.sendPasswordResetEmail(
                        user.getEmail(),
                        passwordResetTokenService.issue(user.getId())
                ));
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        UUID userId = passwordResetTokenService.consume(request.token());
        String operationId;
        try {
            operationId = securityStateService.beginTransition(userId);
        } catch (RuntimeException exception) {
            throw new UserSecurityException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Security service is temporarily unavailable"
            );
        }
        boolean completionRegistered = false;
        try {
            User user = userRepository.findByIdForUpdate(userId)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
            if (!user.isActive()) {
                throw new UserSecurityException(HttpStatus.FORBIDDEN, "User account is inactive");
            }
            user.setPassword(passwordEncoder.encode(request.newPassword()));
            user.setSecurityVersion(user.getSecurityVersion() + 1L);
            refreshTokenService.revokeAllForUser(userId);
            registerTransitionCompletion(
                    userId,
                    operationId,
                    new SecurityState(true, user.getSecurityVersion())
            );
            completionRegistered = true;
        } finally {
            if (!completionRegistered) {
                securityStateService.cancelTransition(userId, operationId);
            }
        }
    }

    private MyProfileResponse toProfile(User user) {
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

    @Override
    @Transactional
    public void changePassword(
            UUID currentUserId,
            ChangePasswordRequest request
    ) {
        String operationId;
        try {
            operationId = securityStateService.beginTransition(currentUserId);
        } catch (IllegalStateException exception) {
            throw new UserSecurityException(
                    HttpStatus.CONFLICT,
                    "Account security information is already being updated"
            );
        } catch (RuntimeException exception) {
            throw new UserSecurityException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Security service is temporarily unavailable"
            );
        }

        boolean completionRegistered = false;
        try {
            User user = userRepository.findByIdForUpdate(currentUserId)
                    .orElseThrow(() -> new UserNotFoundException("User not found"));
            if (!user.isActive()) {
                throw new UserSecurityException(HttpStatus.FORBIDDEN, "User account is inactive");
            }
            if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                throw new UserSecurityException(HttpStatus.BAD_REQUEST, "Current password is incorrect");
            }
            if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
                throw new UserSecurityException(
                        HttpStatus.BAD_REQUEST,
                        "New password must be different from the current password"
                );
            }

            user.setPassword(passwordEncoder.encode(request.newPassword()));
            user.setSecurityVersion(user.getSecurityVersion() + 1L);
            refreshTokenService.revokeAllForUser(currentUserId);
            registerTransitionCompletion(
                    currentUserId,
                    operationId,
                    new SecurityState(user.isActive(), user.getSecurityVersion())
            );
            completionRegistered = true;
        } finally {
            if (!completionRegistered) {
                securityStateService.cancelTransition(currentUserId, operationId);
            }
        }
    }

    private void registerTransitionCompletion(
            UUID userId,
            String operationId,
            SecurityState committedState
    ) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        securityStateService.completeTransition(
                                userId,
                                operationId,
                                committedState
                        );
                    }

                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) {
                            securityStateService.cancelTransition(userId, operationId);
                        }
                    }
                }
        );
    }
}
