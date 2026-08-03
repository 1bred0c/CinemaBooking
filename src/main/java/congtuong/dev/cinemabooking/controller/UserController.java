package congtuong.dev.cinemabooking.controller;

import congtuong.dev.cinemabooking.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> deactivate(@PathVariable UUID userId) {
        userService.deactivate(userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{userId}/activate")
    public ResponseEntity<Void> activate(@PathVariable UUID userId) {
        userService.activate(userId);
        return ResponseEntity.noContent().build();
    }
}
