package congtuong.dev.cinemabooking.controller;

import congtuong.dev.cinemabooking.dto.request.CinemaCreateRequest;
import congtuong.dev.cinemabooking.dto.response.CinemaResponse;
import congtuong.dev.cinemabooking.dto.request.CinemaUpdateRequest;
import congtuong.dev.cinemabooking.service.CinemaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/cinemas")
@RequiredArgsConstructor
public class CinemaController {

    private final CinemaService cinemaService;

    @GetMapping
    public ResponseEntity<List<CinemaResponse>> getAllCinemas() {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(cinemaService.getCinemas());
    }

    @GetMapping("/active")
    public ResponseEntity<List<CinemaResponse>> getActiveCinemas() {
        return ResponseEntity.ok(cinemaService.getActiveCinemas());
    }

    @PostMapping
    public ResponseEntity<CinemaResponse> createCinema(@Valid @RequestBody CinemaCreateRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cinemaService.createCinema(request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CinemaResponse> updateCinema(@PathVariable UUID id, @Valid @RequestBody CinemaUpdateRequest request) {
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(cinemaService.updateCinema(id, request));
    }

    @PatchMapping("/deactivate/{id}")
    public ResponseEntity<CinemaResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(cinemaService.deactivateCinema(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CinemaResponse> getCinema(
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(
                cinemaService.getCinema(id)
        );
    }

    @PatchMapping("/activate/{id}")
    public ResponseEntity<CinemaResponse> activateCinema(@PathVariable UUID id) {
        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(cinemaService.activateCinema(id));
    }
}
