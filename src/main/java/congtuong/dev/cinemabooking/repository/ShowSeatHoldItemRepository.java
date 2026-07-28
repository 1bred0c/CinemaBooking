package congtuong.dev.cinemabooking.repository;

import congtuong.dev.cinemabooking.entity.ShowSeatHoldItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ShowSeatHoldItemRepository extends JpaRepository<ShowSeatHoldItem, UUID> {

    @Query("""
            select i from ShowSeatHoldItem i
            join fetch i.showSeat ss
            join fetch ss.seat
            join fetch ss.showtime
            where i.showSeatHold.id = :holdId
            order by ss.id
            """)
    List<ShowSeatHoldItem> findAllByShowSeatHoldId(@Param("holdId") UUID holdId);
}
