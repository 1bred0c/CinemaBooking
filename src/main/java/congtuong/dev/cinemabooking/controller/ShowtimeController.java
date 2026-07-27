package congtuong.dev.cinemabooking.controller;

import congtuong.dev.cinemabooking.dto.request.ShowtimeCreateRequest;
import congtuong.dev.cinemabooking.dto.request.ShowtimeFilterRequest;
import congtuong.dev.cinemabooking.dto.request.ShowtimeUpdateRequest;
import congtuong.dev.cinemabooking.dto.response.ShowtimeResponse;
import congtuong.dev.cinemabooking.service.ShowtimeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/showtimes")
@RequiredArgsConstructor
public class ShowtimeController {
    private final ShowtimeService showtimeService;

    @GetMapping
    public List<ShowtimeResponse> getShowtimes(@ModelAttribute ShowtimeFilterRequest filter) {
        return showtimeService.getShowtimes(filter);
    }

    @GetMapping("/{showtimeId}")
    public ShowtimeResponse getShowtime(@PathVariable UUID showtimeId) {
        return showtimeService.getShowtimeById(showtimeId);
    }

    @PostMapping
    public ResponseEntity<ShowtimeResponse> createShowtime(
            @Valid @RequestBody ShowtimeCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(showtimeService.createShowtime(request));
    }

    @PatchMapping("/{showtimeId}")
    public ShowtimeResponse updateShowtime(
            @PathVariable UUID showtimeId,
            @Valid @RequestBody ShowtimeUpdateRequest request) {
        return showtimeService.updateShowtime(showtimeId, request);
    }

    @PatchMapping("/{showtimeId}/activate")
    public ShowtimeResponse activateShowtime(@PathVariable UUID showtimeId) {
        return showtimeService.activateShowtime(showtimeId);
    }

    @DeleteMapping("/{showtimeId}")
    public ResponseEntity<Void> deactivateShowtime(@PathVariable UUID showtimeId) {
        showtimeService.deactivateShowtime(showtimeId);
        return ResponseEntity.noContent().build();
    }
}
