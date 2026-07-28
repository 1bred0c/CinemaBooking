package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.response.ShowSeatResponse;
import congtuong.dev.cinemabooking.entity.Seat;
import congtuong.dev.cinemabooking.entity.ShowSeat;
import congtuong.dev.cinemabooking.entity.ShowTime;
import congtuong.dev.cinemabooking.entity.enums.SeatType;
import congtuong.dev.cinemabooking.entity.enums.ShowSeatStatus;
import congtuong.dev.cinemabooking.exception.ShowSeatException;
import congtuong.dev.cinemabooking.exception.ShowtimeException;
import congtuong.dev.cinemabooking.repository.SeatRepository;
import congtuong.dev.cinemabooking.repository.ShowSeatRepository;
import congtuong.dev.cinemabooking.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowSeatServiceImpl implements ShowSeatService {
    private static final BigDecimal PREMIUM_SURCHARGE =
            BigDecimal.valueOf(50_000L);
    private static final BigDecimal COUPLE_SURCHARGE =
            BigDecimal.valueOf(100_000L);

    private final ShowSeatRepository showSeatRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;

    @Override
    public List<ShowSeatResponse> getSeatsByShowtime(UUID showtimeId) {
        if (!showtimeRepository.existsById(showtimeId)) {
            throw new ShowtimeException("Showtime not found");
        }
        return showSeatRepository.findAllByShowtimeId(showtimeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void generateShowSeatsForShowtime(UUID showtimeId) {
        ShowTime showtime = showtimeRepository.findByIdAndActiveTrue(showtimeId)
                .orElseThrow(() -> new ShowtimeException("Active showtime not found"));

        if (showSeatRepository.existsByShowtimeId(showtimeId)) {
            throw new ShowSeatException(
                    HttpStatus.CONFLICT,
                    "Show seats have already been generated for this showtime"
            );
        }

        List<Seat> seats = seatRepository.findAllActiveByRoomId(showtime.getRoom().getId());
        List<ShowSeat> showSeats = seats.stream()
                .map(seat -> ShowSeat.builder()
                        .showtime(showtime)
                        .seat(seat)
                        .status(ShowSeatStatus.AVAILABLE)
                        .price(calculateShowSeatPrice(showtime, seat))
                        .build())
                .toList();

        showSeatRepository.saveAll(showSeats);
    }

    private BigDecimal calculateShowSeatPrice(ShowTime showtime, Seat seat) {
        BigDecimal surcharge = switch (seat.getType()) {
            case STANDARD -> BigDecimal.ZERO;
            case PREMIUM -> PREMIUM_SURCHARGE;
            case COUPLE -> COUPLE_SURCHARGE;
        };

        return showtime.getBasePrice().add(surcharge);
    }

    private ShowSeatResponse toResponse(ShowSeat showSeat) {
        Seat seat = showSeat.getSeat();
        return new ShowSeatResponse(
                showSeat.getId(),
                showSeat.getShowtime().getId(),
                seat.getId(),
                seat.getRow(),
                seat.getNumber(),
                seat.getType(),
                showSeat.getStatus(),
                showSeat.getPrice()
        );
    }
}
