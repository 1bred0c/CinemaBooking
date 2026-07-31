package congtuong.dev.cinemabooking.service.expiration;

import congtuong.dev.cinemabooking.entity.ShowSeat;
import congtuong.dev.cinemabooking.entity.ShowSeatHold;
import congtuong.dev.cinemabooking.entity.ShowSeatHoldItem;
import congtuong.dev.cinemabooking.entity.enums.ShowSeatHoldStatus;
import congtuong.dev.cinemabooking.entity.enums.ShowSeatStatus;
import congtuong.dev.cinemabooking.repository.ShowSeatHoldItemRepository;
import congtuong.dev.cinemabooking.repository.ShowSeatHoldRepository;
import congtuong.dev.cinemabooking.repository.ShowSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ShowSeatHoldExpirationWorker {

    private final ShowSeatHoldRepository showSeatHoldRepository;
    private final ShowSeatHoldItemRepository showSeatHoldItemRepository;
    private final ShowSeatRepository showSeatRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean expire(UUID holdId, Instant now) {
        ShowSeatHold hold = showSeatHoldRepository
                .findByIdForUpdate(holdId)
                .orElse(null);

        if (hold == null
                || hold.getStatus() != ShowSeatHoldStatus.ACTIVE
                || hold.getExpiresAt().isAfter(now)) {
            return false;
        }

        List<UUID> showSeatIds = showSeatHoldItemRepository
                .findAllByShowSeatHoldId(holdId)
                .stream()
                .map(ShowSeatHoldItem::getShowSeat)
                .map(ShowSeat::getId)
                .sorted()
                .toList();
        List<ShowSeat> lockedSeats =
                showSeatRepository.findAllByIdForUpdate(showSeatIds);
        if (showSeatIds.isEmpty()
                || lockedSeats.size() != showSeatIds.size()) {
            throw new IllegalStateException(
                    "Expiring hold has missing seat records"
            );
        }

        lockedSeats.stream()
                .filter(seat -> seat.getStatus() == ShowSeatStatus.HELD)
                .forEach(seat ->
                        seat.setStatus(ShowSeatStatus.AVAILABLE)
                );
        hold.setStatus(ShowSeatHoldStatus.EXPIRED);
        return true;
    }
}
