package congtuong.dev.cinemabooking.ai.tool;

import congtuong.dev.cinemabooking.ai.tool.dto.SeatAvailabilityToolResponse;
import congtuong.dev.cinemabooking.ai.tool.dto.ShowtimeDetailsToolResponse;
import congtuong.dev.cinemabooking.ai.tool.dto.ShowtimeSearchToolResponse;
import congtuong.dev.cinemabooking.ai.tool.dto.TicketPricesToolResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class CinemaBookingTools {

    private final CinemaToolQueryService queryService;

    @Tool(description = """
            Search real, currently scheduled CinemaBooking showtimes for a movie.
            Use this whenever the user asks when or where a movie is showing.
            movieTitle must be an explicit movie title supplied by the user;
            never pass a theme, plot, mood, genre, or descriptive phrase here.
            When retrieved context contains a Movie ID, use
            searchShowtimesByMovieId instead. date must be ISO yyyy-MM-dd when supplied.
            period may be ANY, MORNING, AFTERNOON, EVENING, or NIGHT.
            Do not use this tool merely to describe or recommend a movie.
            """)
    public ShowtimeSearchToolResponse searchShowtimes(
            @ToolParam(description = "Movie title to search")
            String movieTitle,
            @ToolParam(
                    description = "Optional cinema name or partial cinema name",
                    required = false
            )
            String cinemaName,
            @ToolParam(
                    description = "Optional show date in ISO yyyy-MM-dd; defaults to today",
                    required = false
            )
            String date,
            @ToolParam(
                    description = "Optional period: ANY, MORNING, AFTERNOON, EVENING, NIGHT",
                    required = false
            )
            String period
    ) {
        LocalDate parsedDate = parseDate(date);
        if (date != null && !date.isBlank() && parsedDate == null) {
            return new ShowtimeSearchToolResponse(
                    false,
                    "Invalid date. Use ISO yyyy-MM-dd",
                    List.of()
            );
        }
        log.debug(
                "Tool searchShowtimes: movieTitle='{}', cinemaName='{}', date={}, period={}",
                movieTitle,
                cinemaName,
                parsedDate,
                period
        );
        return queryService.searchShowtimes(
                movieTitle,
                cinemaName,
                parsedDate,
                ShowtimePeriod.fromNullable(period)
        );
    }

    @Tool(description = """
            Search real, currently scheduled showtimes using an exact
            CinemaBooking movie UUID. Prefer this tool when retrieved movie
            context supplies Movie IDs, especially for requests that describe
            a theme, plot, mood, or genre instead of naming a movie.
            date must be ISO yyyy-MM-dd when supplied. period may be ANY,
            MORNING, AFTERNOON, EVENING, or NIGHT.
            """)
    public ShowtimeSearchToolResponse searchShowtimesByMovieId(
            @ToolParam(description = "Movie UUID from CinemaBooking movie data")
            String movieId,
            @ToolParam(
                    description = "Optional show date in ISO yyyy-MM-dd; defaults to today",
                    required = false
            )
            String date,
            @ToolParam(
                    description = "Optional period: ANY, MORNING, AFTERNOON, EVENING, NIGHT",
                    required = false
            )
            String period
    ) {
        UUID id = parseUuid(movieId);
        LocalDate parsedDate = parseDate(date);
        if (id == null) {
            return new ShowtimeSearchToolResponse(
                    false, "Invalid movie ID", List.of()
            );
        }
        if (date != null && !date.isBlank() && parsedDate == null) {
            return new ShowtimeSearchToolResponse(
                    false, "Invalid date. Use ISO yyyy-MM-dd", List.of()
            );
        }
        log.debug(
                "Tool searchShowtimesByMovieId: movieId={}, date={}, period={}",
                id, parsedDate, period
        );
        return queryService.searchShowtimesByMovieId(
                id, parsedDate, ShowtimePeriod.fromNullable(period)
        );
    }

    @Tool(description = """
            List all real, currently scheduled CinemaBooking showtimes for a
            date and optional period. Use this when the user asks what movies
            are showing today, tomorrow, or on a specified date without naming
            or describing a particular movie. Recommend only from these results.
            date must be ISO yyyy-MM-dd. period may be ANY, MORNING, AFTERNOON,
            EVENING, or NIGHT.
            """)
    public ShowtimeSearchToolResponse searchShowtimesByDate(
            @ToolParam(description = "Show date in ISO yyyy-MM-dd")
            String date,
            @ToolParam(
                    description = "Optional period: ANY, MORNING, AFTERNOON, EVENING, NIGHT",
                    required = false
            )
            String period
    ) {
        LocalDate parsedDate = parseDate(date);
        if (parsedDate == null) {
            return new ShowtimeSearchToolResponse(
                    false, "Invalid date. Use ISO yyyy-MM-dd", List.of()
            );
        }
        log.debug(
                "Tool searchShowtimesByDate: date={}, period={}",
                parsedDate, period
        );
        return queryService.searchShowtimesByDate(
                parsedDate, ShowtimePeriod.fromNullable(period)
        );
    }

    @Tool(description = """
            Get verified details for one active showtime by its UUID. Use only a
            showtimeId previously supplied by CinemaBooking search results.
            """)
    public ShowtimeDetailsToolResponse getShowtimeDetails(
            @ToolParam(description = "CinemaBooking showtime UUID")
            String showtimeId
    ) {
        UUID id = parseUuid(showtimeId);
        if (id == null) {
            return new ShowtimeDetailsToolResponse(
                    false,
                    "Invalid showtime ID",
                    null
            );
        }
        log.debug("Tool getShowtimeDetails: showtimeId={}", id);
        return queryService.getShowtimeDetails(id);
    }

    @Tool(description = """
            Get a point-in-time summary of available, held, booked, and blocked
            seats for a showtime. Also returns availability and price ranges by
            seat type. Use only a verified CinemaBooking showtime UUID.
            """)
    public SeatAvailabilityToolResponse getSeatAvailability(
            @ToolParam(description = "CinemaBooking showtime UUID")
            String showtimeId
    ) {
        UUID id = parseUuid(showtimeId);
        if (id == null) {
            return new SeatAvailabilityToolResponse(
                    false,
                    "Invalid showtime ID",
                    null,
                    null,
                    null,
                    0,
                    0,
                    0,
                    0,
                    0,
                    List.of()
            );
        }
        log.debug("Tool getSeatAvailability: showtimeId={}", id);
        return queryService.getSeatAvailability(id);
    }

    @Tool(description = """
            Get current ticket prices grouped by seat type for one showtime.
            Use only a verified CinemaBooking showtime UUID. Never estimate a
            ticket price without calling this tool.
            """)
    public TicketPricesToolResponse getTicketPrices(
            @ToolParam(description = "CinemaBooking showtime UUID")
            String showtimeId
    ) {
        UUID id = parseUuid(showtimeId);
        if (id == null) {
            return new TicketPricesToolResponse(
                    false,
                    "Invalid showtime ID",
                    null,
                    null,
                    null,
                    List.of()
            );
        }
        log.debug("Tool getTicketPrices: showtimeId={}", id);
        return queryService.getTicketPrices(id);
    }

    private LocalDate parseDate(String date) {
        if (date == null || date.isBlank()) return null;
        try {
            return LocalDate.parse(date.trim());
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private UUID parseUuid(String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return UUID.fromString(value.trim());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
