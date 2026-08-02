package congtuong.dev.cinemabooking.ai.tool;

import congtuong.dev.cinemabooking.dto.response.ShowtimeSeatAvailability;
import congtuong.dev.cinemabooking.entity.Cinema;
import congtuong.dev.cinemabooking.entity.Movie;
import congtuong.dev.cinemabooking.entity.Room;
import congtuong.dev.cinemabooking.entity.Seat;
import congtuong.dev.cinemabooking.entity.ShowSeat;
import congtuong.dev.cinemabooking.entity.ShowTime;
import congtuong.dev.cinemabooking.entity.enums.RoomStatus;
import congtuong.dev.cinemabooking.entity.enums.SeatType;
import congtuong.dev.cinemabooking.entity.enums.ShowSeatStatus;
import congtuong.dev.cinemabooking.entity.enums.ShowtimeStatus;
import congtuong.dev.cinemabooking.repository.ShowSeatRepository;
import congtuong.dev.cinemabooking.repository.ShowtimeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CinemaToolQueryServiceImplTest {

    @Mock private ShowtimeRepository showtimeRepository;
    @Mock private ShowSeatRepository showSeatRepository;

    @Test
    void searchShowtimesReturnsVerifiedAvailability() {
        CinemaToolQueryService service = service();
        ShowTime showtime = showtime();
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        when(showtimeRepository.searchBookableForToolWithoutCinema(
                eq("Interstellar"),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(ShowtimeStatus.SCHEDULED),
                eq(RoomStatus.ACTIVE)
        )).thenReturn(List.of(showtime));
        when(showSeatRepository.countByShowtimeIdsAndStatus(
                List.of(showtime.getId()),
                ShowSeatStatus.AVAILABLE
        )).thenReturn(List.of(new ShowtimeSeatAvailability(
                showtime.getId(),
                42
        )));

        var response = service.searchShowtimes(
                "Interstellar",
                null,
                tomorrow,
                ShowtimePeriod.EVENING
        );

        assertTrue(response.success());
        assertEquals(1, response.showtimes().size());
        assertEquals(42, response.showtimes().get(0).availableSeats());
        assertEquals("Cinema Q1", response.showtimes().get(0).cinemaName());
    }

    @Test
    void searchShowtimesByMovieIdUsesExactRetrievedMovie() {
        CinemaToolQueryService service = service();
        ShowTime showtime = showtime();
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        when(showtimeRepository.searchBookableForToolByMovieId(
                eq(showtime.getMovie().getId()),
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(ShowtimeStatus.SCHEDULED),
                eq(RoomStatus.ACTIVE)
        )).thenReturn(List.of(showtime));
        when(showSeatRepository.countByShowtimeIdsAndStatus(
                List.of(showtime.getId()), ShowSeatStatus.AVAILABLE
        )).thenReturn(List.of(new ShowtimeSeatAvailability(
                showtime.getId(), 12
        )));

        var response = service.searchShowtimesByMovieId(
                showtime.getMovie().getId(), tomorrow, ShowtimePeriod.ANY
        );

        assertTrue(response.success());
        assertEquals("Interstellar", response.showtimes().get(0).movieTitle());
        assertEquals(12, response.showtimes().get(0).availableSeats());
    }

    @Test
    void searchShowtimesByDateStartsFromActuallyScheduledMovies() {
        CinemaToolQueryService service = service();
        ShowTime showtime = showtime();
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        when(showtimeRepository.searchBookableForToolByDate(
                any(LocalDateTime.class),
                any(LocalDateTime.class),
                eq(ShowtimeStatus.SCHEDULED),
                eq(RoomStatus.ACTIVE)
        )).thenReturn(List.of(showtime));
        when(showSeatRepository.countByShowtimeIdsAndStatus(
                List.of(showtime.getId()), ShowSeatStatus.AVAILABLE
        )).thenReturn(List.of(new ShowtimeSeatAvailability(
                showtime.getId(), 8
        )));

        var response = service.searchShowtimesByDate(
                tomorrow, ShowtimePeriod.ANY
        );

        assertTrue(response.success());
        assertEquals(1, response.showtimes().size());
        assertEquals(8, response.showtimes().get(0).availableSeats());
    }

    @Test
    void seatAvailabilityCountsStatusesAndGroupsPrices() {
        CinemaToolQueryService service = service();
        ShowTime showtime = showtime();
        when(showtimeRepository.findActiveDetailsById(showtime.getId()))
                .thenReturn(Optional.of(showtime));
        when(showSeatRepository.findAllByShowtimeId(showtime.getId()))
                .thenReturn(List.of(
                        showSeat(showtime, SeatType.STANDARD,
                                ShowSeatStatus.AVAILABLE, "100000"),
                        showSeat(showtime, SeatType.STANDARD,
                                ShowSeatStatus.BOOKED, "100000"),
                        showSeat(showtime, SeatType.PREMIUM,
                                ShowSeatStatus.HELD, "150000")
                ));

        var response = service.getSeatAvailability(showtime.getId());

        assertTrue(response.success());
        assertEquals(3, response.totalSeats());
        assertEquals(1, response.availableSeats());
        assertEquals(1, response.heldSeats());
        assertEquals(1, response.bookedSeats());
        assertEquals(2, response.bySeatType().size());
    }

    private CinemaToolQueryService service() {
        return new CinemaToolQueryServiceImpl(
                showtimeRepository,
                showSeatRepository
        );
    }

    private ShowTime showtime() {
        Movie movie = Movie.builder()
                .id(UUID.randomUUID())
                .title("Interstellar")
                .durationMinutes(169)
                .active(true)
                .build();
        Cinema cinema = Cinema.builder()
                .id(UUID.randomUUID())
                .name("Cinema Q1")
                .address("District 1")
                .isActive(true)
                .build();
        Room room = Room.builder()
                .id(UUID.randomUUID())
                .name("Room 1")
                .status(RoomStatus.ACTIVE)
                .cinema(cinema)
                .build();
        LocalDateTime start = LocalDate.now()
                .plusDays(1)
                .atTime(19, 30);
        return ShowTime.builder()
                .id(UUID.randomUUID())
                .movie(movie)
                .room(room)
                .startTime(start)
                .endTime(start.plusMinutes(169))
                .basePrice(new BigDecimal("100000"))
                .status(ShowtimeStatus.SCHEDULED)
                .active(true)
                .build();
    }

    private ShowSeat showSeat(
            ShowTime showtime,
            SeatType type,
            ShowSeatStatus status,
            String price
    ) {
        Seat seat = Seat.builder()
                .id(UUID.randomUUID())
                .row("A")
                .number(1)
                .type(type)
                .isActive(true)
                .build();
        return ShowSeat.builder()
                .id(UUID.randomUUID())
                .showtime(showtime)
                .seat(seat)
                .status(status)
                .price(new BigDecimal(price))
                .build();
    }
}
