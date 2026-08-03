package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.ShowtimeCreateRequest;
import congtuong.dev.cinemabooking.dto.request.ShowtimeFilterRequest;
import congtuong.dev.cinemabooking.dto.request.ShowtimeUpdateRequest;
import congtuong.dev.cinemabooking.dto.response.ShowtimeResponse;
import congtuong.dev.cinemabooking.dto.response.ShowtimeBrowseResponse;
import congtuong.dev.cinemabooking.dto.response.ShowtimeSeatAvailability;
import congtuong.dev.cinemabooking.entity.*;
import congtuong.dev.cinemabooking.entity.enums.RoomStatus;
import congtuong.dev.cinemabooking.entity.enums.ShowtimeStatus;
import congtuong.dev.cinemabooking.entity.enums.ShowSeatStatus;
import congtuong.dev.cinemabooking.exception.ShowtimeException;
import congtuong.dev.cinemabooking.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowtimeServiceImpl implements ShowtimeService {
    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final RoomRepository roomRepository;
    private final ShowSeatRepository showSeatRepository;
    private final ShowSeatService showSeatService;

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
        showSeatService.generateShowSeatsForShowtime(savedShowtime.getId());
        return toResponse(savedShowtime);
    }

    @Override
    public ShowtimeResponse getShowtimeById(UUID showtimeId) {
        return toResponse(findShowtime(showtimeId));
    }

    @Override
    public Page<ShowtimeResponse> getShowtimes(
            ShowtimeFilterRequest filter,
            Pageable pageable
    ) {
        ShowtimeFilterRequest appliedFilter = filter == null
                ? new ShowtimeFilterRequest(null, null, null, null, null, null, null)
                : filter;
        return showtimeRepository.findAllByFilter(
                        appliedFilter.movieId(),
                        appliedFilter.cinemaId(),
                        appliedFilter.roomId(),
                        appliedFilter.startTimeFrom(),
                        appliedFilter.startTimeTo(),
                        appliedFilter.status(),
                        appliedFilter.active(),
                        pageable)
                .map(this::toResponse);
    }

    @Override
    public List<ShowtimeBrowseResponse> getBookableShowtimesByMovie(
            UUID movieId,
            UUID cinemaId,
            LocalDate date
    ) {
        Movie movie = findActiveMovie(movieId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startTime = date == null
                ? now
                : date.atStartOfDay();
        if (startTime.isBefore(now)) {
            startTime = now;
        }
        LocalDateTime endTime = date == null
                ? null
                : date.plusDays(1).atStartOfDay();
        if (endTime != null && !endTime.isAfter(now)) {
            return List.of();
        }

        List<ShowTime> showtimes = showtimeRepository.findBookableByMovie(
                movie.getId(),
                cinemaId,
                startTime,
                endTime,
                ShowtimeStatus.SCHEDULED
        );
        if (showtimes.isEmpty()) {
            return List.of();
        }

        Map<UUID, Long> availableSeats = showSeatRepository
                .countByShowtimeIdsAndStatus(
                        showtimes.stream().map(ShowTime::getId).toList(),
                        ShowSeatStatus.AVAILABLE
                )
                .stream()
                .collect(java.util.stream.Collectors.toMap(
                        ShowtimeSeatAvailability::showtimeId,
                        ShowtimeSeatAvailability::availableSeats
                ));

        return showtimes.stream()
                .map(showtime -> toBrowseResponse(
                        showtime,
                        availableSeats.getOrDefault(showtime.getId(), 0L)
                ))
                .toList();
    }

    @Override
    @Transactional
    public ShowtimeResponse updateShowtime(UUID showtimeId, ShowtimeUpdateRequest request) {
        ShowTime showtime = findShowtime(showtimeId);

        if (request.roomId() != null
                && !Objects.equals(request.roomId(), showtime.getRoom().getId())
                && showSeatRepository.existsByShowtimeId(showtimeId)) {
            throw new ShowtimeException(
                    "Cannot change room after show seats have been generated"
            );
        }

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

    private ShowtimeBrowseResponse toBrowseResponse(
            ShowTime showtime,
            long availableSeats
    ) {
        Room room = showtime.getRoom();
        Cinema cinema = room.getCinema();
        return new ShowtimeBrowseResponse(
                showtime.getId(),
                showtime.getMovie().getId(),
                showtime.getMovie().getTitle(),
                cinema.getId(),
                cinema.getName(),
                cinema.getAddress(),
                room.getId(),
                room.getName(),
                room.getRoomType(),
                showtime.getStartTime(),
                showtime.getEndTime(),
                showtime.getBasePrice(),
                availableSeats
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
