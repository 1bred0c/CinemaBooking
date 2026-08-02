package congtuong.dev.cinemabooking.notification;

import congtuong.dev.cinemabooking.dto.response.NotificationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Service
@Slf4j
public class NotificationPushService {

    private static final long SSE_TIMEOUT_MILLIS = 30 * 60 * 1000L;

    private final ConcurrentHashMap<UUID, Set<SseEmitter>> connections =
            new ConcurrentHashMap<>();

    public SseEmitter subscribe(UUID userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MILLIS);
        connections.computeIfAbsent(
                userId,
                ignored -> new CopyOnWriteArraySet<>()
        ).add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(error -> remove(userId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("connected"));
        } catch (IOException exception) {
            remove(userId, emitter);
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    public void send(UUID userId, NotificationResponse notification) {
        Set<SseEmitter> emitters = connections.get(userId);
        if (emitters == null) {
            return;
        }
        emitters.forEach(emitter -> {
            try {
                emitter.send(SseEmitter.event()
                        .id(notification.id().toString())
                        .name("notification")
                        .data(notification));
            } catch (IOException | IllegalStateException exception) {
                log.debug(
                        "Removing closed SSE connection for userId={}",
                        userId
                );
                remove(userId, emitter);
                emitter.complete();
            }
        });
    }

    private void remove(UUID userId, SseEmitter emitter) {
        connections.computeIfPresent(userId, (ignored, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
