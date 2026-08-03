package congtuong.dev.cinemabooking.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TicketCheckInRequest(@NotBlank String qrToken) {
}
