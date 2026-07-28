package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.ShowSeatHoldCreateRequest;
import congtuong.dev.cinemabooking.dto.response.ShowSeatHoldResponse;

import java.util.UUID;

public interface ShowSeatHoldService {
    ShowSeatHoldResponse createHold(ShowSeatHoldCreateRequest request, UUID currentUserId);
    ShowSeatHoldResponse getMyHold(UUID holdId, UUID currentUserId);
    void cancelHold(UUID holdId, UUID currentUserId);
    int expireActiveHolds();
}
