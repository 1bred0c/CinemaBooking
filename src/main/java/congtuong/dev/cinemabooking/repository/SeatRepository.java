package congtuong.dev.cinemabooking.repository;

import congtuong.dev.cinemabooking.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeatRepository extends JpaRepository<Seat, UUID> {
    List<Seat> findByRoomId(UUID roomId);
    Optional<Seat> findByRoomIdAndNumberAndRow(UUID roomId, Integer number, String row);
}
