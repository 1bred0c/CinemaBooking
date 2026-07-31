package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.SeatCreateRequest;
import congtuong.dev.cinemabooking.dto.request.SeatLayoutCreateRequest;
import congtuong.dev.cinemabooking.dto.request.SeatUpdateRequest;
import congtuong.dev.cinemabooking.dto.response.SeatResponse;

import java.util.List;
import java.util.UUID;

public interface SeatService {
    List<SeatResponse> getAllSeats();
    SeatResponse createSeat(SeatCreateRequest seat);
    List<SeatResponse> createSeatLayout(SeatLayoutCreateRequest request);
    SeatResponse getSeatById(UUID seatId);
    void deactivateSeat(UUID seatId);
    SeatResponse updateSeat(UUID seatId, SeatUpdateRequest seat);
    List<SeatResponse> getSeatsByRoomId(UUID roomId);
}
