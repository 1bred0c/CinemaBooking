package congtuong.dev.cinemabooking.ai.tool;

import congtuong.dev.cinemabooking.ai.tool.dto.SeatAvailabilityToolResponse;
import congtuong.dev.cinemabooking.ai.tool.dto.ShowtimeDetailsToolResponse;
import congtuong.dev.cinemabooking.ai.tool.dto.ShowtimeSearchToolResponse;
import congtuong.dev.cinemabooking.ai.tool.dto.TicketPricesToolResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface CinemaToolQueryService {

    ShowtimeSearchToolResponse searchShowtimes(
            String movieTitle,
            String cinemaName,
            LocalDate date,
            ShowtimePeriod period
    );

    ShowtimeSearchToolResponse searchShowtimesByMovieId(
            UUID movieId,
            LocalDate date,
            ShowtimePeriod period
    );

    ShowtimeSearchToolResponse searchShowtimesByDate(
            LocalDate date,
            ShowtimePeriod period
    );

    ShowtimeDetailsToolResponse getShowtimeDetails(UUID showtimeId);

    SeatAvailabilityToolResponse getSeatAvailability(UUID showtimeId);

    TicketPricesToolResponse getTicketPrices(UUID showtimeId);
}
