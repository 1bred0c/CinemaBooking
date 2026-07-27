package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.GenreCreateRequest;
import congtuong.dev.cinemabooking.dto.request.GenreUpdateRequest;
import congtuong.dev.cinemabooking.dto.response.GenreResponse;

import java.util.List;
import java.util.UUID;

public interface GenreService {
    List<GenreResponse> getGenres();
    GenreResponse getGenre(UUID id);
    GenreResponse createGenre(GenreCreateRequest request);
    GenreResponse updateGenre(UUID id, GenreUpdateRequest request);
    void deactivateGenre(UUID id);
}
