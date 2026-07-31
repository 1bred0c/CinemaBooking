package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.CinemaCreateRequest;
import congtuong.dev.cinemabooking.dto.response.CinemaResponse;
import congtuong.dev.cinemabooking.dto.request.CinemaUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface CinemaService {
    List<CinemaResponse> getCinemas();
    List<CinemaResponse> getActiveCinemas();
    CinemaResponse getCinema(UUID id);
    CinemaResponse createCinema(CinemaCreateRequest request);
    CinemaResponse updateCinema(UUID id, CinemaUpdateRequest request);
    CinemaResponse deactivateCinema(UUID id);
    CinemaResponse activateCinema(UUID id);
}
