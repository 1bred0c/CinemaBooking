package congtuong.dev.cinemabooking.controller;

import congtuong.dev.cinemabooking.dto.request.TicketCheckInRequest;
import congtuong.dev.cinemabooking.dto.response.TicketResponse;
import congtuong.dev.cinemabooking.security.jwt.CustomUserDetails;
import congtuong.dev.cinemabooking.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;

    @GetMapping("/booking/{bookingId}")
    public TicketResponse getTicket(
            @PathVariable UUID bookingId,
            @AuthenticationPrincipal CustomUserDetails currentUser
    ) {
        return ticketService.getTicket(
                currentUser.getUser().getId(),
                bookingId
        );
    }

    @PostMapping("/check-in")
    public TicketResponse checkIn(
            @Valid @RequestBody TicketCheckInRequest request
    ) {
        return ticketService.checkIn(request.qrToken());
    }
}
