package congtuong.dev.cinemabooking.controller;

import congtuong.dev.cinemabooking.dto.response.ShowSeatResponse;
import congtuong.dev.cinemabooking.service.ShowSeatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/showtimes/{showtimeId}/seats")
@RequiredArgsConstructor
public class ShowSeatController {
    private final ShowSeatService showSeatService;

    @GetMapping
    public List<ShowSeatResponse> getSeatsByShowtime(@PathVariable UUID showtimeId) {
        return showSeatService.getSeatsByShowtime(showtimeId);
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> generateShowSeats(@PathVariable UUID showtimeId) {
        showSeatService.generateShowSeatsForShowtime(showtimeId);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
