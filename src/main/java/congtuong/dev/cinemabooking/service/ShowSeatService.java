package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.response.ShowSeatResponse;

import java.util.List;
import java.util.UUID;

public interface ShowSeatService {
    List<ShowSeatResponse> getSeatsByShowtime(UUID showtimeId);
    void generateShowSeatsForShowtime(UUID showtimeId);
}
