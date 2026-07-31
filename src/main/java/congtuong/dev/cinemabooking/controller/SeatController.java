package congtuong.dev.cinemabooking.controller;

import congtuong.dev.cinemabooking.dto.request.SeatCreateRequest;
import congtuong.dev.cinemabooking.dto.request.SeatLayoutCreateRequest;
import congtuong.dev.cinemabooking.dto.request.SeatUpdateRequest;
import congtuong.dev.cinemabooking.dto.response.SeatResponse;
import congtuong.dev.cinemabooking.service.SeatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/seats")
@RequiredArgsConstructor
public class SeatController {

    private final SeatService seatService;

    @GetMapping
    public ResponseEntity<List<SeatResponse>> getSeats(@RequestParam(required = false) UUID roomId) {
        if (roomId != null) {
            return ResponseEntity.ok(seatService.getSeatsByRoomId(roomId));
        }
        return ResponseEntity.ok(seatService.getAllSeats());
    }

    @GetMapping("/{seatId}")
    public ResponseEntity<SeatResponse> getSeat(@PathVariable UUID seatId) {
        return ResponseEntity.ok(seatService.getSeatById(seatId));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeatResponse> createSeat(@Valid @RequestBody SeatCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seatService.createSeat(request));
    }

    @PostMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SeatResponse>> createSeatLayout(
            @Valid @RequestBody SeatLayoutCreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(seatService.createSeatLayout(request));
    }

    @PatchMapping("/{seatId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SeatResponse> updateSeat(@PathVariable UUID seatId,
                                                   @Valid @RequestBody SeatUpdateRequest request) {
        return ResponseEntity.ok(seatService.updateSeat(seatId, request));
    }

    @DeleteMapping("/{seatId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteSeat(@PathVariable UUID seatId) {
        seatService.deactivateSeat(seatId);
        return ResponseEntity.noContent().build();
    }
}
