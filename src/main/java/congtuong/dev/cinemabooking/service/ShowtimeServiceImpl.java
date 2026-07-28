package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.ShowtimeCreateRequest;
import congtuong.dev.cinemabooking.dto.request.ShowtimeFilterRequest;
import congtuong.dev.cinemabooking.dto.request.ShowtimeUpdateRequest;
import congtuong.dev.cinemabooking.dto.response.ShowtimeResponse;
import congtuong.dev.cinemabooking.entity.Movie;
import congtuong.dev.cinemabooking.entity.Room;
import congtuong.dev.cinemabooking.entity.ShowTime;
import congtuong.dev.cinemabooking.entity.enums.RoomStatus;
import congtuong.dev.cinemabooking.entity.enums.ShowtimeStatus;
import congtuong.dev.cinemabooking.exception.ShowtimeException;
import congtuong.dev.cinemabooking.repository.MovieRepository;
import congtuong.dev.cinemabooking.repository.RoomRepository;
import congtuong.dev.cinemabooking.repository.ShowtimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowtimeServiceImpl implements ShowtimeService {
    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;

    @Override
    @Transactional
    public ShowtimeResponse createShowtime(ShowtimeCreateRequest request) {
        Movie movie = findActiveMovie(request.movieId());
        Room room = findActiveRoomForUpdate(request.roomId());
        validateBasePrice(request.basePrice());
        validateStartTime(request.startTime());
        LocalDateTime startTime = request.startTime();
        LocalDateTime endTime = request.startTime().plusMinutes(movie.getDurationMinutes());


        boolean hasConflict =
                showtimeRepository.existsOverlappingShowtime(
                        room.getId(),
                        startTime,
                        endTime,
                        ShowtimeStatus.CANCELLED
                );
        if (hasConflict) {
            throw new ShowtimeException("Room is already booked for the selected time.");
        }

        ShowTime showtime = ShowTime.builder()
                .movie(movie)
                .room(room)
                .startTime(request.startTime())
                .endTime(endTime)
                .basePrice(request.basePrice())
                .status(request.status() == null ? ShowtimeStatus.SCHEDULED : request.status())
                .build();

        ShowTime savedShowtime = showtimeRepository.save(showtime);
        // TODO: Generate ShowSeat after successful Showtime creation

        return toResponse(savedShowtime);
    }

    @Override
    public ShowtimeResponse getShowtimeById(UUID showtimeId) {
        return toResponse(findShowtime(showtimeId));
    }

    @Override
    public List<ShowtimeResponse> getShowtimes(ShowtimeFilterRequest filter) {
        ShowtimeFilterRequest appliedFilter = filter == null
                ? new ShowtimeFilterRequest(null, null, null, null, null, null)
                : filter;
        return showtimeRepository.findAllByFilter(
                        appliedFilter.movieId(),
                        appliedFilter.roomId(),
                        appliedFilter.startTimeFrom(),
                        appliedFilter.startTimeTo(),
                        appliedFilter.status(),
                        appliedFilter.active())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ShowtimeResponse updateShowtime(UUID showtimeId, ShowtimeUpdateRequest request) {
        ShowTime showtime = findShowtime(showtimeId);

        if (request.movieId() != null) {
            showtime.setMovie(findActiveMovie(request.movieId()));
        }
        if (request.roomId() != null) {
            showtime.setRoom(findActiveRoom(request.roomId()));
        }
        if (request.startTime() != null) {
            validateStartTime(request.startTime());
            showtime.setStartTime(request.startTime());
        }
        if (request.movieId() != null || request.startTime() != null) {
            showtime.setEndTime(
                    showtime.getStartTime().plusMinutes(showtime.getMovie().getDurationMinutes())
            );
        }
        if (request.basePrice() != null) {
            validateBasePrice(request.basePrice());
            showtime.setBasePrice(request.basePrice());
        }
        if (request.status() != null) {
            showtime.setStatus(request.status());
        }

        validateActiveMovie(showtime.getMovie());
        showtime.setRoom(findActiveRoomForUpdate(showtime.getRoom().getId()));

        boolean hasConflict =
                showtimeRepository.existsOverlappingShowtimeExcludingId(
                        showtime.getId(),
                        showtime.getRoom().getId(),
                        showtime.getStartTime(),
                        showtime.getEndTime(),
                        ShowtimeStatus.CANCELLED
                );
        if (hasConflict) {
            throw new ShowtimeException("Room is already booked for the selected time.");
        }


        return toResponse(showtime);
    }

    @Override
    @Transactional
    public void deactivateShowtime(UUID showtimeId) {
        findShowtime(showtimeId).setActive(false);
    }

    @Override
    @Transactional
    public ShowtimeResponse activateShowtime(UUID showtimeId) {
        ShowTime showtime = findShowtime(showtimeId);
        showtime.setActive(true);
        return toResponse(showtime);
    }

    private ShowTime findShowtime(UUID showtimeId) {
        return showtimeRepository.findById(showtimeId)
                .orElseThrow(() -> new ShowtimeException("Showtime not found"));
    }

    private Movie findActiveMovie(UUID movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new ShowtimeException("Movie not found"));
        validateActiveMovie(movie);
        return movie;
    }

    private void validateActiveMovie(Movie movie) {
        if (!movie.isActive()) {
            throw new ShowtimeException("Movie is inactive");
        }
    }

    private Room findActiveRoom(UUID roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new ShowtimeException("Room not found"));
        validateActiveRoom(room);
        return room;
    }

    private Room findActiveRoomForUpdate(UUID roomId) {
        Room room = roomRepository.findByIdForUpdate(roomId, RoomStatus.ACTIVE)
                .orElseThrow(() -> new ShowtimeException("Room not found"));
        validateActiveRoom(room);
        return room;
    }

    private void validateActiveRoom(Room room) {
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new ShowtimeException("Room is inactive");
        }
    }

    private void validateBasePrice(BigDecimal basePrice) {
        if (basePrice == null || basePrice.signum() <= 0) {
            throw new ShowtimeException("Base price must be positive");
        }
    }

    private ShowtimeResponse toResponse(ShowTime showtime) {
        return new ShowtimeResponse(
                showtime.getId(),
                showtime.getMovie().getId(),
                showtime.getMovie().getTitle(),
                showtime.getRoom().getId(),
                showtime.getRoom().getName(),
                showtime.getStartTime(),
                showtime.getEndTime(),
                showtime.getBasePrice(),
                showtime.getStatus(),
                showtime.isActive(),
                showtime.getCreatedAt(),
                showtime.getUpdatedAt()
        );
    }

    private void validateStartTime(LocalDateTime startTime) {
        if (startTime == null) {
            throw new ShowtimeException("Start time is required.");
        }

        if (!startTime.isAfter(LocalDateTime.now())) {
            throw new ShowtimeException(
                    "Showtime must start in the future."
            );
        }
    }
}
