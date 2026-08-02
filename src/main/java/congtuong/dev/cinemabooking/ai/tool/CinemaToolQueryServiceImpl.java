package congtuong.dev.cinemabooking.ai.tool;

import congtuong.dev.cinemabooking.ai.tool.dto.SeatAvailabilityToolResponse;
import congtuong.dev.cinemabooking.ai.tool.dto.SeatTypeAvailability;
import congtuong.dev.cinemabooking.ai.tool.dto.SeatTypePrice;
import congtuong.dev.cinemabooking.ai.tool.dto.ShowtimeDetailsToolResponse;
import congtuong.dev.cinemabooking.ai.tool.dto.ShowtimeSearchToolResponse;
import congtuong.dev.cinemabooking.ai.tool.dto.ShowtimeToolItem;
import congtuong.dev.cinemabooking.ai.tool.dto.TicketPricesToolResponse;
import congtuong.dev.cinemabooking.dto.response.ShowtimeSeatAvailability;
import congtuong.dev.cinemabooking.entity.Cinema;
import congtuong.dev.cinemabooking.entity.Room;
import congtuong.dev.cinemabooking.entity.ShowSeat;
import congtuong.dev.cinemabooking.entity.ShowTime;
import congtuong.dev.cinemabooking.entity.enums.RoomStatus;
import congtuong.dev.cinemabooking.entity.enums.SeatType;
import congtuong.dev.cinemabooking.entity.enums.ShowSeatStatus;
import congtuong.dev.cinemabooking.entity.enums.ShowtimeStatus;
import congtuong.dev.cinemabooking.repository.ShowSeatRepository;
import congtuong.dev.cinemabooking.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CinemaToolQueryServiceImpl implements CinemaToolQueryService {

    private final ShowtimeRepository showtimeRepository;
    private final ShowSeatRepository showSeatRepository;

    @Override
    public ShowtimeSearchToolResponse searchShowtimes(
            String movieTitle,
            String cinemaName,
            LocalDate date,
            ShowtimePeriod period
    ) {
        if (movieTitle == null || movieTitle.isBlank()) {
            return new ShowtimeSearchToolResponse(
                    false,
                    "Movie title is required to search showtimes",
                    List.of()
            );
        }

        LocalDate requestedDate = date == null ? LocalDate.now() : date;
        if (requestedDate.isBefore(LocalDate.now())) {
            return new ShowtimeSearchToolResponse(
                    true,
                    "The requested date is in the past",
                    List.of()
            );
        }

        ShowtimePeriod appliedPeriod = period == null
                ? ShowtimePeriod.ANY
                : period;
        LocalDateTime startTime = requestedDate.atTime(appliedPeriod.start());
        LocalDateTime endTime = requestedDate.atTime(appliedPeriod.end());
        LocalDateTime now = LocalDateTime.now();
        if (startTime.isBefore(now)) {
            startTime = now;
        }
        if (!endTime.isAfter(startTime)) {
            return new ShowtimeSearchToolResponse(
                    true,
                    "No future showtime remains in the requested period",
                    List.of()
            );
        }

        String normalizedCinemaName = blankToNull(cinemaName);
        List<ShowTime> showtimes = normalizedCinemaName == null
                ? showtimeRepository.searchBookableForToolWithoutCinema(
                        movieTitle.trim(), startTime, endTime,
                        ShowtimeStatus.SCHEDULED, RoomStatus.ACTIVE
                )
                : showtimeRepository.searchBookableForTool(
                        movieTitle.trim(), normalizedCinemaName,
                        startTime, endTime,
                        ShowtimeStatus.SCHEDULED, RoomStatus.ACTIVE
                );
        Map<UUID, Long> availableSeats = availableSeatCounts(showtimes);
        List<ShowtimeToolItem> items = showtimes.stream()
                .map(showtime -> toItem(
                        showtime,
                        availableSeats.getOrDefault(showtime.getId(), 0L)
                ))
                .toList();

        return new ShowtimeSearchToolResponse(
                true,
                items.isEmpty()
                        ? "No matching scheduled showtime was found"
                        : "Found %d matching showtime(s)".formatted(items.size()),
                items
        );
    }

    @Override
    public ShowtimeSearchToolResponse searchShowtimesByMovieId(
            UUID movieId,
            LocalDate date,
            ShowtimePeriod period
    ) {
        if (movieId == null) {
            return new ShowtimeSearchToolResponse(
                    false, "Movie ID is required to search showtimes", List.of()
            );
        }

        LocalDate requestedDate = date == null ? LocalDate.now() : date;
        if (requestedDate.isBefore(LocalDate.now())) {
            return new ShowtimeSearchToolResponse(
                    true, "The requested date is in the past", List.of()
            );
        }
        ShowtimePeriod appliedPeriod = period == null
                ? ShowtimePeriod.ANY
                : period;
        LocalDateTime startTime = requestedDate.atTime(appliedPeriod.start());
        LocalDateTime endTime = requestedDate.atTime(appliedPeriod.end());
        LocalDateTime now = LocalDateTime.now();
        if (startTime.isBefore(now)) startTime = now;
        if (!endTime.isAfter(startTime)) {
            return new ShowtimeSearchToolResponse(
                    true,
                    "No future showtime remains in the requested period",
                    List.of()
            );
        }

        List<ShowTime> showtimes = showtimeRepository
                .searchBookableForToolByMovieId(
                        movieId, startTime, endTime,
                        ShowtimeStatus.SCHEDULED, RoomStatus.ACTIVE
                );
        Map<UUID, Long> availableSeats = availableSeatCounts(showtimes);
        List<ShowtimeToolItem> items = showtimes.stream()
                .map(showtime -> toItem(
                        showtime,
                        availableSeats.getOrDefault(showtime.getId(), 0L)
                ))
                .toList();
        return new ShowtimeSearchToolResponse(
                true,
                items.isEmpty()
                        ? "No matching scheduled showtime was found"
                        : "Found %d matching showtime(s)".formatted(items.size()),
                items
        );
    }

    @Override
    public ShowtimeSearchToolResponse searchShowtimesByDate(
            LocalDate date,
            ShowtimePeriod period
    ) {
        LocalDate requestedDate = date == null ? LocalDate.now() : date;
        if (requestedDate.isBefore(LocalDate.now())) {
            return new ShowtimeSearchToolResponse(
                    true, "The requested date is in the past", List.of()
            );
        }
        ShowtimePeriod appliedPeriod = period == null
                ? ShowtimePeriod.ANY
                : period;
        LocalDateTime startTime = requestedDate.atTime(appliedPeriod.start());
        LocalDateTime endTime = requestedDate.atTime(appliedPeriod.end());
        LocalDateTime now = LocalDateTime.now();
        if (startTime.isBefore(now)) startTime = now;
        if (!endTime.isAfter(startTime)) {
            return new ShowtimeSearchToolResponse(
                    true,
                    "No future showtime remains in the requested period",
                    List.of()
            );
        }

        List<ShowTime> showtimes = showtimeRepository
                .searchBookableForToolByDate(
                        startTime, endTime,
                        ShowtimeStatus.SCHEDULED, RoomStatus.ACTIVE
                );
        Map<UUID, Long> availableSeats = availableSeatCounts(showtimes);
        List<ShowtimeToolItem> items = showtimes.stream()
                .map(showtime -> toItem(
                        showtime,
                        availableSeats.getOrDefault(showtime.getId(), 0L)
                ))
                .toList();
        return new ShowtimeSearchToolResponse(
                true,
                items.isEmpty()
                        ? "No scheduled showtime was found for the requested date"
                        : "Found %d scheduled showtime(s)".formatted(items.size()),
                items
        );
    }

    @Override
    public ShowtimeDetailsToolResponse getShowtimeDetails(UUID showtimeId) {
        if (showtimeId == null) {
            return new ShowtimeDetailsToolResponse(
                    false,
                    "Showtime ID is required",
                    null
            );
        }
        ShowTime showtime = findShowtime(showtimeId);
        if (showtime == null) {
            return new ShowtimeDetailsToolResponse(
                        false,
                        "Scheduled future showtime was not found",
                        null
                );
        }
        return new ShowtimeDetailsToolResponse(
                true,
                "Showtime found",
                toItem(showtime, availableSeatCount(showtime.getId()))
        );
    }

    @Override
    public SeatAvailabilityToolResponse getSeatAvailability(UUID showtimeId) {
        ShowTime showtime = findShowtime(showtimeId);
        if (showtime == null) {
            return emptySeatAvailability(
                    showtimeId,
                    "Active showtime was not found"
            );
        }
        List<ShowSeat> seats = showSeatRepository.findAllByShowtimeId(showtimeId);
        if (seats.isEmpty()) {
            return emptySeatAvailability(
                    showtimeId,
                    "No show seats have been generated for this showtime"
            );
        }

        Map<SeatType, List<ShowSeat>> byType = seats.stream()
                .collect(Collectors.groupingBy(
                        seat -> seat.getSeat().getType(),
                        () -> new EnumMap<>(SeatType.class),
                        Collectors.toList()
                ));
        List<SeatTypeAvailability> typeAvailability = byType.entrySet().stream()
                .map(entry -> toTypeAvailability(entry.getKey(), entry.getValue()))
                .toList();

        return new SeatAvailabilityToolResponse(
                true,
                "Seat availability is a point-in-time snapshot",
                showtimeId,
                showtime.getMovie().getTitle(),
                showtime.getStartTime(),
                seats.size(),
                countStatus(seats, ShowSeatStatus.AVAILABLE),
                countStatus(seats, ShowSeatStatus.HELD),
                countStatus(seats, ShowSeatStatus.BOOKED),
                countStatus(seats, ShowSeatStatus.BLOCKED),
                typeAvailability
        );
    }

    @Override
    public TicketPricesToolResponse getTicketPrices(UUID showtimeId) {
        ShowTime showtime = findShowtime(showtimeId);
        if (showtime == null) {
            return new TicketPricesToolResponse(
                    false,
                    "Active showtime was not found",
                    showtimeId,
                    null,
                    null,
                    List.of()
            );
        }
        List<ShowSeat> seats = showSeatRepository.findAllByShowtimeId(showtimeId);
        Map<SeatType, List<ShowSeat>> byType = seats.stream()
                .collect(Collectors.groupingBy(
                        seat -> seat.getSeat().getType(),
                        () -> new EnumMap<>(SeatType.class),
                        Collectors.toList()
                ));
        List<SeatTypePrice> prices = byType.entrySet().stream()
                .map(entry -> new SeatTypePrice(
                        entry.getKey(),
                        minimumPrice(entry.getValue()),
                        maximumPrice(entry.getValue())
                ))
                .toList();
        return new TicketPricesToolResponse(
                true,
                prices.isEmpty()
                        ? "No show-seat prices are available"
                        : "Prices are read from the current show seats",
                showtimeId,
                showtime.getMovie().getTitle(),
                showtime.getBasePrice(),
                prices
        );
    }

    private ShowTime findShowtime(UUID showtimeId) {
        if (showtimeId == null) return null;
        return showtimeRepository.findActiveDetailsById(showtimeId)
                .filter(showtime -> showtime.getStatus() == ShowtimeStatus.SCHEDULED)
                .filter(showtime -> showtime.getStartTime().isAfter(LocalDateTime.now()))
                .orElse(null);
    }

    private Map<UUID, Long> availableSeatCounts(List<ShowTime> showtimes) {
        if (showtimes.isEmpty()) return Map.of();
        return showSeatRepository.countByShowtimeIdsAndStatus(
                        showtimes.stream().map(ShowTime::getId).toList(),
                        ShowSeatStatus.AVAILABLE
                )
                .stream()
                .collect(Collectors.toMap(
                        ShowtimeSeatAvailability::showtimeId,
                        ShowtimeSeatAvailability::availableSeats
                ));
    }

    private long availableSeatCount(UUID showtimeId) {
        return showSeatRepository.countByShowtimeIdsAndStatus(
                        List.of(showtimeId),
                        ShowSeatStatus.AVAILABLE
                )
                .stream()
                .mapToLong(ShowtimeSeatAvailability::availableSeats)
                .findFirst()
                .orElse(0);
    }

    private ShowtimeToolItem toItem(ShowTime showtime, long availableSeats) {
        Room room = showtime.getRoom();
        Cinema cinema = room.getCinema();
        return new ShowtimeToolItem(
                showtime.getId(),
                showtime.getMovie().getId(),
                showtime.getMovie().getTitle(),
                cinema.getId(),
                cinema.getName(),
                cinema.getAddress(),
                room.getId(),
                room.getName(),
                showtime.getStartTime(),
                showtime.getEndTime(),
                showtime.getBasePrice(),
                availableSeats
        );
    }

    private SeatTypeAvailability toTypeAvailability(
            SeatType type,
            List<ShowSeat> seats
    ) {
        return new SeatTypeAvailability(
                type,
                seats.size(),
                countStatus(seats, ShowSeatStatus.AVAILABLE),
                minimumPrice(seats),
                maximumPrice(seats)
        );
    }

    private long countStatus(List<ShowSeat> seats, ShowSeatStatus status) {
        return seats.stream().filter(seat -> seat.getStatus() == status).count();
    }

    private BigDecimal minimumPrice(List<ShowSeat> seats) {
        return seats.stream()
                .map(ShowSeat::getPrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private BigDecimal maximumPrice(List<ShowSeat> seats) {
        return seats.stream()
                .map(ShowSeat::getPrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }

    private SeatAvailabilityToolResponse emptySeatAvailability(
            UUID showtimeId,
            String message
    ) {
        return new SeatAvailabilityToolResponse(
                false,
                message,
                showtimeId,
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

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
