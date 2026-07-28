package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.config.BookingProperties;
import congtuong.dev.cinemabooking.dto.request.ShowSeatHoldCreateRequest;
import congtuong.dev.cinemabooking.dto.response.ShowSeatHoldItemResponse;
import congtuong.dev.cinemabooking.dto.response.ShowSeatHoldResponse;
import congtuong.dev.cinemabooking.entity.Room;
import congtuong.dev.cinemabooking.entity.Seat;
import congtuong.dev.cinemabooking.entity.ShowSeat;
import congtuong.dev.cinemabooking.entity.ShowSeatHold;
import congtuong.dev.cinemabooking.entity.ShowSeatHoldItem;
import congtuong.dev.cinemabooking.entity.ShowTime;
import congtuong.dev.cinemabooking.entity.User;
import congtuong.dev.cinemabooking.entity.enums.ShowSeatHoldStatus;
import congtuong.dev.cinemabooking.entity.enums.ShowSeatStatus;
import congtuong.dev.cinemabooking.exception.SeatNotAvailableException;
import congtuong.dev.cinemabooking.exception.ShowSeatHoldException;
import congtuong.dev.cinemabooking.repository.ShowSeatHoldItemRepository;
import congtuong.dev.cinemabooking.repository.ShowSeatHoldRepository;
import congtuong.dev.cinemabooking.repository.ShowSeatRepository;
import congtuong.dev.cinemabooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShowSeatHoldServiceImpl implements ShowSeatHoldService {
    private final ShowSeatRepository showSeatRepository;
    private final ShowSeatHoldRepository showSeatHoldRepository;
    private final ShowSeatHoldItemRepository showSeatHoldItemRepository;
    private final UserRepository userRepository;
    private final BookingProperties bookingProperties;

    @Override
    @Transactional
    public ShowSeatHoldResponse createHold(
            ShowSeatHoldCreateRequest request,
            UUID currentUserId
    ) {
        List<UUID> requestedIds = validateAndGetDistinctIds(request);
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new ShowSeatHoldException("User not found"));

        List<ShowSeat> lockedSeats = showSeatRepository.findAllByIdForUpdate(requestedIds);

        if (lockedSeats.size() != requestedIds.size()) {
            throw new ShowSeatHoldException("Seats not found");
        }

        ShowTime currentShowTime = lockedSeats.get(0).getShowtime();
        Room room = currentShowTime.getRoom();

        if (!currentShowTime.isActive()) {
            throw new ShowSeatHoldException(
                    HttpStatus.CONFLICT,
                    "Cannot hold seats for an inactive showtime"
            );
        }

        for (ShowSeat showSeat : lockedSeats) {
            if (!showSeat.getShowtime().getId().equals(currentShowTime.getId())) {
                throw new ShowSeatHoldException(
                        HttpStatus.BAD_REQUEST,
                        "Selected seats must belong to the same showtime"
                );
            }
            if (!showSeat.getSeat().getRoom().getId().equals(room.getId())) {
                throw new ShowSeatHoldException(
                        HttpStatus.BAD_REQUEST,
                        "Selected seat does not belong to the showtime room"
                );
            }
            if (showSeat.getStatus() != ShowSeatStatus.AVAILABLE) {
                throw new SeatNotAvailableException(
                        "One or more selected seats are unavailable"
                );
            }
        }

        ShowSeatHold hold = ShowSeatHold.builder()
                .user(currentUser)
                .showtime(currentShowTime)
                .status(ShowSeatHoldStatus.ACTIVE)
                .expiresAt(calculateExpiresAt())
                .build();
        showSeatHoldRepository.save(hold);

        List<ShowSeatHoldItem> holdItems = new ArrayList<>();
        for (ShowSeat showSeat : lockedSeats) {
            ShowSeatHoldItem holdItem = ShowSeatHoldItem.builder()
                    .showSeatHold(hold)
                    .showSeat(showSeat)
                    .heldPrice(showSeat.getPrice())
                    .build();
            holdItems.add(holdItem);
        }
        showSeatHoldItemRepository.saveAll(holdItems);

        for (ShowSeat showSeat : lockedSeats) {
            showSeat.setStatus(ShowSeatStatus.HELD);
        }

        return toResponse(hold, holdItems);
    }

    @Override
    public ShowSeatHoldResponse getMyHold(UUID holdId, UUID currentUserId) {
        ShowSeatHold hold = findOwnedHold(holdId, currentUserId);
        List<ShowSeatHoldItem> items =
                showSeatHoldItemRepository.findAllByShowSeatHoldId(holdId);
        return toResponse(hold, items);
    }

    @Override
    @Transactional
    public void cancelHold(UUID holdId, UUID currentUserId) {
        ShowSeatHold hold = showSeatHoldRepository
                .findByIdAndUserIdForUpdate(holdId, currentUserId)
                .orElseThrow(() -> new ShowSeatHoldException(
                        HttpStatus.NOT_FOUND,
                        "Hold not found"
                ));

        if (hold.getStatus() != ShowSeatHoldStatus.ACTIVE) {
            throw new ShowSeatHoldException(
                    HttpStatus.CONFLICT,
                    "Only active holds can be cancelled"
            );
        }

        List<ShowSeatHoldItem> holdItems =
                showSeatHoldItemRepository.findAllByShowSeatHoldId(holdId);
        if (holdItems.isEmpty()) {
            throw new ShowSeatHoldException(
                    HttpStatus.CONFLICT,
                    "Cannot cancel a hold without seats"
            );
        }

        List<UUID> showSeatIds = holdItems.stream()
                .map(item -> item.getShowSeat().getId())
                .sorted()
                .toList();
        List<ShowSeat> lockedSeats =
                showSeatRepository.findAllByIdForUpdate(showSeatIds);

        if (lockedSeats.size() != showSeatIds.size()) {
            throw new ShowSeatHoldException(
                    HttpStatus.CONFLICT,
                    "One or more held seats no longer exist"
            );
        }

        for (ShowSeat showSeat : lockedSeats) {
            if (showSeat.getStatus() != ShowSeatStatus.HELD) {
                throw new ShowSeatHoldException(
                        HttpStatus.CONFLICT,
                        "One or more seats are no longer held"
                );
            }
            showSeat.setStatus(ShowSeatStatus.AVAILABLE);
        }

        hold.setStatus(ShowSeatHoldStatus.CANCELLED);
    }

    @Override
    @Transactional
    public int expireActiveHolds() {
        List<ShowSeatHold> expiredHolds =
                showSeatHoldRepository.findAllByStatusAndExpiresAtBefore(
                        ShowSeatHoldStatus.ACTIVE,
                        Instant.now()
                );
        if (expiredHolds.isEmpty()) {
            return 0;
        }

        // TODO EXPIRATION 1:
        // Process expired holds in bounded batches.

        // TODO EXPIRATION 2:
        // Pessimistically lock each hold and its ShowSeat rows.

        // TODO EXPIRATION 3:
        // Re-check ACTIVE status and expiresAt after locking for idempotency.

        // TODO EXPIRATION 4:
        // Protect the legal ACTIVE -> EXPIRED transition from payment confirmation.

        // TODO EXPIRATION 5:
        // Release only seats that are still HELD by the expiring hold, then return
        // the number of holds transitioned to EXPIRED.

        throw new UnsupportedOperationException(
                "Core seat hold expiration logic must be implemented"
        );
    }

    private List<UUID> validateAndGetDistinctIds(ShowSeatHoldCreateRequest request) {
        if (request == null || request.showSeatIds() == null || request.showSeatIds().isEmpty()) {
            throw new ShowSeatHoldException(
                    HttpStatus.BAD_REQUEST,
                    "At least one show seat must be selected"
            );
        }
        if (request.showSeatIds().stream().anyMatch(id -> id == null)) {
            throw new ShowSeatHoldException(
                    HttpStatus.BAD_REQUEST,
                    "Show seat IDs must not contain null values"
            );
        }

        Set<UUID> distinctIds = new HashSet<>(request.showSeatIds());
        if (distinctIds.size() != request.showSeatIds().size()) {
            throw new ShowSeatHoldException(
                    HttpStatus.BAD_REQUEST,
                    "Duplicate show seat IDs are not allowed"
            );
        }
        return distinctIds.stream().sorted().toList();
    }

    private ShowSeatHold findOwnedHold(UUID holdId, UUID currentUserId) {
        return showSeatHoldRepository.findByIdAndUserId(holdId, currentUserId)
                .orElseThrow(() -> new ShowSeatHoldException("Show seat hold not found"));
    }

    private Instant calculateExpiresAt() {
        return Instant.now().plus(
                Duration.ofMinutes(bookingProperties.seatHoldDurationMinutes())
        );
    }

    private ShowSeatHoldResponse toResponse(
            ShowSeatHold hold,
            List<ShowSeatHoldItem> items
    ) {
        List<ShowSeatHoldItemResponse> seats = items.stream()
                .map(this::toItemResponse)
                .toList();
        BigDecimal totalAmount = items.stream()
                .map(ShowSeatHoldItem::getHeldPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new ShowSeatHoldResponse(
                hold.getId(),
                hold.getShowtime().getId(),
                hold.getStatus(),
                hold.getExpiresAt(),
                totalAmount,
                seats
        );
    }

    private ShowSeatHoldItemResponse toItemResponse(ShowSeatHoldItem item) {
        ShowSeat showSeat = item.getShowSeat();
        Seat seat = showSeat.getSeat();
        return new ShowSeatHoldItemResponse(
                showSeat.getId(),
                seat.getId(),
                seat.getRow(),
                seat.getNumber(),
                seat.getType(),
                item.getHeldPrice()
        );
    }
}
