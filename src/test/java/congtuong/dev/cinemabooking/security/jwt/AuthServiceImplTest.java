package congtuong.dev.cinemabooking.security.jwt;

import congtuong.dev.cinemabooking.dto.request.RegisterRequest;
import congtuong.dev.cinemabooking.entity.User;
import congtuong.dev.cinemabooking.messaging.event.UserRegisteredEvent;
import congtuong.dev.cinemabooking.repository.UserRepository;
import congtuong.dev.cinemabooking.service.OutboxEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private OutboxEventService outboxEventService;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void registerAppendsUserRegisteredEventToOutbox() {
        UUID userId = UUID.randomUUID();
        RegisterRequest request = new RegisterRequest(
                "0912345678",
                "  USER@example.com ",
                "secret123",
                "Cinema User",
                LocalDate.of(2000, 1, 1)
        );

        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(userId);
            return user;
        });

        authService.register(request);

        ArgumentCaptor<UserRegisteredEvent> eventCaptor =
                ArgumentCaptor.forClass(UserRegisteredEvent.class);
        verify(outboxEventService).append(eventCaptor.capture());

        UserRegisteredEvent event = eventCaptor.getValue();
        assertThat(event.userId()).isEqualTo(userId);
        assertThat(event.email()).isEqualTo("user@example.com");
        assertThat(event.fullName()).isEqualTo("Cinema User");
        assertThat(event.version()).isEqualTo(1);
    }

    @Test
    void getMyProfileLoadsCurrentUserFromDatabase() {
        UUID userId = UUID.randomUUID();
        LocalDateTime createdAt = LocalDateTime.of(
                2026,
                7,
                30,
                10,
                0
        );
        LocalDateTime updatedAt = createdAt.plusDays(1);
        User user = User.builder()
                .id(userId)
                .phoneNumber("0912345678")
                .email("user@example.com")
                .fullname("Cinema User")
                .birthDate(LocalDate.of(2000, 1, 1))
                .role(congtuong.dev.cinemabooking.entity.enums.Role.CUSTOMER)
                .isActive(true)
                .createAt(Timestamp.valueOf(createdAt))
                .updateAt(Timestamp.valueOf(updatedAt))
                .build();
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        var response = authService.getMyProfile(userId);

        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(2000, 1, 1));
        assertThat(response.active()).isTrue();
        assertThat(response.createdAt()).isEqualTo(createdAt);
        assertThat(response.updatedAt()).isEqualTo(updatedAt);
    }
}
