package congtuong.dev.cinemabooking.ai.tool.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record TicketPricesToolResponse(
        boolean success,
        String message,
        UUID showtimeId,
        String movieTitle,
        BigDecimal basePrice,
        List<SeatTypePrice> prices
) {
}
