package congtuong.dev.cinemabooking.notification;

import congtuong.dev.cinemabooking.entity.Notification;
import congtuong.dev.cinemabooking.entity.User;
import congtuong.dev.cinemabooking.messaging.event.UserRegisteredEvent;
import congtuong.dev.cinemabooking.repository.NotificationRepository;
import congtuong.dev.cinemabooking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEventServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;

    @Test
    void duplicateEventDoesNotCreateAnotherNotification() {
        UUID eventId = UUID.randomUUID();
        UserRegisteredEvent event = event(eventId, UUID.randomUUID());
        when(notificationRepository.existsById(eventId)).thenReturn(true);
        NotificationEventService service = new NotificationEventService(
                notificationRepository,
                userRepository
        );

        assertThat(service.create(event)).isEmpty();

        verify(notificationRepository, never()).save(
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void userEventCreatesWelcomeNotification() {
        UUID eventId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UserRegisteredEvent event = event(eventId, userId);
        User user = User.builder().id(userId).build();
        when(notificationRepository.existsById(eventId)).thenReturn(false);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(notificationRepository.save(
                org.mockito.ArgumentMatchers.any(Notification.class)
        )).thenAnswer(invocation -> invocation.getArgument(0));
        NotificationEventService service = new NotificationEventService(
                notificationRepository,
                userRepository
        );

        assertThat(service.create(event)).isPresent();

        ArgumentCaptor<Notification> captor =
                ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(eventId);
        assertThat(captor.getValue().getRecipient()).isSameAs(user);
        assertThat(captor.getValue().getReadAt()).isNull();
    }

    private UserRegisteredEvent event(UUID eventId, UUID userId) {
        return new UserRegisteredEvent(
                eventId,
                userId,
                "user@example.com",
                "Cinema User",
                Instant.parse("2026-07-31T10:00:00Z"),
                1
        );
    }
}
