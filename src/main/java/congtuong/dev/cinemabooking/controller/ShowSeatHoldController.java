package congtuong.dev.cinemabooking.controller;

import congtuong.dev.cinemabooking.dto.request.ShowSeatHoldCreateRequest;
import congtuong.dev.cinemabooking.dto.response.ShowSeatHoldResponse;
import congtuong.dev.cinemabooking.security.jwt.CustomUserDetails;
import congtuong.dev.cinemabooking.service.ShowSeatHoldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/show-seat-holds")
@RequiredArgsConstructor
public class ShowSeatHoldController {
    private final ShowSeatHoldService showSeatHoldService;

    @PostMapping
    public ResponseEntity<ShowSeatHoldResponse> createHold(
            @Valid @RequestBody ShowSeatHoldCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        ShowSeatHoldResponse response = showSeatHoldService.createHold(
                request,
                currentUser.getUser().getId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{holdId}")
    public ShowSeatHoldResponse getMyHold(
            @PathVariable UUID holdId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return showSeatHoldService.getMyHold(
                holdId,
                currentUser.getUser().getId()
        );
    }

    @GetMapping("/active")
    public ShowSeatHoldResponse getActiveHold(
            @RequestParam(required = false) UUID showtimeId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return showSeatHoldService.getActiveHold(
                currentUser.getUser().getId(),
                showtimeId
        );
    }

    @DeleteMapping("/{holdId}")
    public ResponseEntity<Void> cancelHold(
            @PathVariable UUID holdId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        showSeatHoldService.cancelHold(
                holdId,
                currentUser.getUser().getId()
        );
        return ResponseEntity.noContent().build();
    }
}
