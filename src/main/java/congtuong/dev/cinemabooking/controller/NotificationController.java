package congtuong.dev.cinemabooking.controller;

import congtuong.dev.cinemabooking.dto.response.NotificationResponse;
import congtuong.dev.cinemabooking.dto.response.UnreadNotificationCountResponse;
import congtuong.dev.cinemabooking.notification.NotificationPushService;
import congtuong.dev.cinemabooking.security.jwt.CustomUserDetails;
import congtuong.dev.cinemabooking.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;

import static org.springframework.data.domain.Sort.Direction.DESC;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationPushService notificationPushService;

    @GetMapping
    public ResponseEntity<Page<NotificationResponse>> getMyNotifications(
            @AuthenticationPrincipal CustomUserDetails currentUser,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = DESC
            ) Pageable pageable
    ) {
        return ResponseEntity.ok(
                notificationService.getMyNotifications(
                        currentUser.getUser().getId(),
                        pageable
                )
        );
    }

    @GetMapping("/unread-count")
    public ResponseEntity<UnreadNotificationCountResponse> getUnreadCount(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(new UnreadNotificationCountResponse(
                notificationService.getUnreadCount(
                        currentUser.getUser().getId()
                )
        ));
    }

    @GetMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter stream(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return notificationPushService.subscribe(
                currentUser.getUser().getId()
        );
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markRead(
            @PathVariable UUID notificationId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ResponseEntity.ok(notificationService.markRead(
                currentUser.getUser().getId(),
                notificationId
        ));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllRead(
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        int updated = notificationService.markAllRead(
                currentUser.getUser().getId()
        );
        return ResponseEntity.ok(Map.of("updated", updated));
    }
}
