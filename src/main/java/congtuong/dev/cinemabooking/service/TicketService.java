package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.response.TicketResponse;

import java.util.UUID;

public interface TicketService {
    TicketResponse getTicket(UUID currentUserId, UUID bookingId);
    TicketResponse checkIn(String qrToken);
}
