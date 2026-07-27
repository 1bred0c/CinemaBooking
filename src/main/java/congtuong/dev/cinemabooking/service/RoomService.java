package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.RoomCreateRequest;
import congtuong.dev.cinemabooking.dto.response.RoomResponse;
import congtuong.dev.cinemabooking.dto.request.RoomUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface RoomService {
    List<RoomResponse> findAllRooms();
    RoomResponse createRoom (RoomCreateRequest roomCreateRequest);
    RoomResponse getRoom (UUID roomId);
    RoomResponse updateRoom (UUID roomId, RoomUpdateRequest roomUpdateRequest);
    RoomResponse deactivateRoom (UUID roomId);
}
