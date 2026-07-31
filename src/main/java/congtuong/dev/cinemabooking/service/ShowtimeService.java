package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.ShowtimeCreateRequest;
import congtuong.dev.cinemabooking.dto.request.ShowtimeFilterRequest;
import congtuong.dev.cinemabooking.dto.request.ShowtimeUpdateRequest;
import congtuong.dev.cinemabooking.dto.response.ShowtimeResponse;

import java.util.List;
import java.time.LocalDate;
import java.util.UUID;
import congtuong.dev.cinemabooking.dto.response.ShowtimeBrowseResponse;

public interface ShowtimeService {
    ShowtimeResponse createShowtime(ShowtimeCreateRequest request);
    ShowtimeResponse getShowtimeById(UUID showtimeId);
    List<ShowtimeResponse> getShowtimes(ShowtimeFilterRequest filter);
    List<ShowtimeBrowseResponse> getBookableShowtimesByMovie(
            UUID movieId,
            UUID cinemaId,
            LocalDate date
    );
    ShowtimeResponse updateShowtime(UUID showtimeId, ShowtimeUpdateRequest request);
    void deactivateShowtime(UUID showtimeId);
    ShowtimeResponse activateShowtime(UUID showtimeId);
}
