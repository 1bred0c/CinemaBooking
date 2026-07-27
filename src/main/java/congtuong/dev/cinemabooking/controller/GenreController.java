package congtuong.dev.cinemabooking.controller;

import congtuong.dev.cinemabooking.dto.request.GenreCreateRequest;
import congtuong.dev.cinemabooking.dto.request.GenreUpdateRequest;
import congtuong.dev.cinemabooking.dto.response.GenreResponse;
import congtuong.dev.cinemabooking.service.GenreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/genres")
@RequiredArgsConstructor
public class GenreController {
    private final GenreService genreService;

    @GetMapping
    public List<GenreResponse> getGenres() {
        return genreService.getGenres();
    }

    @GetMapping("/{id}")
    public GenreResponse getGenre(@PathVariable UUID id) {
        return genreService.getGenre(id);
    }

    @PostMapping
    public ResponseEntity<GenreResponse> createGenre(@Valid @RequestBody GenreCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(genreService.createGenre(request));
    }

    @PatchMapping("/{id}")
    public GenreResponse updateGenre(@PathVariable UUID id,
                                     @Valid @RequestBody GenreUpdateRequest request) {
        return genreService.updateGenre(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateGenre(@PathVariable UUID id) {
        genreService.deactivateGenre(id);
        return ResponseEntity.noContent().build();
    }
}
