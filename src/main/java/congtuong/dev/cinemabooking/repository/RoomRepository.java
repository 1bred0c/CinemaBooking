package congtuong.dev.cinemabooking.repository;

import congtuong.dev.cinemabooking.entity.Room;
import congtuong.dev.cinemabooking.entity.enums.RoomStatus;
import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(
            @QueryHint(
                    name = "jakarta.persistence.lock.timeout",
                    value = "3000"
            )
    )
    @Query("""
    SELECT r
    FROM Room r
    WHERE r.id = :id
      AND r.status = :status
""")
    Optional<Room> findByIdForUpdate(
            @Param("id") UUID id,
            @Param("status") RoomStatus status
    );
}
