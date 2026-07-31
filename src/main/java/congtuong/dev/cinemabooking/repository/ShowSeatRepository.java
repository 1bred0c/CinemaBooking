package congtuong.dev.cinemabooking.repository;

import congtuong.dev.cinemabooking.entity.ShowSeat;
import congtuong.dev.cinemabooking.dto.response.ShowtimeSeatAvailability;
import congtuong.dev.cinemabooking.entity.enums.ShowSeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ShowSeatRepository extends JpaRepository<ShowSeat, UUID> {

    @Query("""
            select ss from ShowSeat ss
            join fetch ss.showtime st
            join fetch ss.seat s
            where st.id = :showtimeId
            order by s.row, s.number
            """)
    List<ShowSeat> findAllByShowtimeId(@Param("showtimeId") UUID showtimeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select ss from ShowSeat ss
            join fetch ss.showtime st
            join fetch ss.seat s
            join fetch s.room
            where ss.id in :ids
            order by ss.id
            """)
    List<ShowSeat> findAllByIdForUpdate(@Param("ids") Collection<UUID> ids);

    boolean existsByShowtimeId(UUID showtimeId);

    @Query("""
            select new congtuong.dev.cinemabooking.dto.response.ShowtimeSeatAvailability(
                ss.showtime.id,
                count(ss)
            )
            from ShowSeat ss
            where ss.showtime.id in :showtimeIds
              and ss.status = :status
            group by ss.showtime.id
            """)
    List<ShowtimeSeatAvailability> countByShowtimeIdsAndStatus(
            @Param("showtimeIds") Collection<UUID> showtimeIds,
            @Param("status") ShowSeatStatus status
    );
}
