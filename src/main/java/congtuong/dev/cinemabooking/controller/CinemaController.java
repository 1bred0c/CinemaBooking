package congtuong.dev.cinemabooking.controller;

import congtuong.dev.cinemabooking.dto.request.CinemaCreateRequest;
import congtuong.dev.cinemabooking.dto.response.CinemaResponse;
import congtuong.dev.cinemabooking.dto.request.CinemaUpdateRequest;
import congtuong.dev.cinemabooking.service.CinemaService;
import congtuong.dev.cinemabooking.service.ShowtimeService;
import congtuong.dev.cinemabooking.dto.request.ShowtimeFilterRequest;
import congtuong.dev.cinemabooking.dto.response.ShowtimeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/cinemas")
@RequiredArgsConstructor
public class CinemaController {

    private final CinemaService cinemaService;
    private final ShowtimeService showtimeService;

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

    @GetMapping("/{id}/showtimes")
    public Page<ShowtimeResponse> getCinemaShowtimes(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID movieId,
            @RequestParam(required = false) LocalDate date,
            @PageableDefault(
                    size = 20,
                    sort = "startTime",
                    direction = Sort.Direction.ASC
            ) Pageable pageable
    ) {
        return showtimeService.getShowtimes(
                new ShowtimeFilterRequest(
                        movieId,
                        id,
                        null,
                        date == null ? null : date.atStartOfDay(),
                        date == null ? null : date.plusDays(1).atStartOfDay(),
                        null,
                        true
                ),
                pageable
        );
    }
}
