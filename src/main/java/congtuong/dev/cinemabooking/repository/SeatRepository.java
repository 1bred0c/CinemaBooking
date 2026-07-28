package congtuong.dev.cinemabooking.repository;

import congtuong.dev.cinemabooking.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SeatRepository extends JpaRepository<Seat, UUID> {
    List<Seat> findByRoomId(UUID roomId);
    @Query("""
            select s from Seat s
            where s.room.id = :roomId
              and s.isActive = true
            order by s.row, s.number
            """)
    List<Seat> findAllActiveByRoomId(@Param("roomId") UUID roomId);
    Optional<Seat> findByRoomIdAndNumberAndRow(UUID roomId, Integer number, String row);
}
