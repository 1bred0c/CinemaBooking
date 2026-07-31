package congtuong.dev.cinemabooking.repository;

import congtuong.dev.cinemabooking.entity.ShowTime;
import congtuong.dev.cinemabooking.entity.enums.ShowtimeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShowtimeRepository extends JpaRepository<ShowTime, UUID> {

    Optional<ShowTime> findByIdAndActiveTrue(UUID id);

    @Query("""
            select s from ShowTime s
            join fetch s.movie
            join fetch s.room
            where (:movieId is null or s.movie.id = :movieId)
              and (:roomId is null or s.room.id = :roomId)
              and (:startTimeFrom is null or s.startTime >= :startTimeFrom)
              and (:startTimeTo is null or s.startTime <= :startTimeTo)
              and (:status is null or s.status = :status)
              and (:active is null or s.active = :active)
            order by s.startTime
            """)
    List<ShowTime> findAllByFilter(
            @Param("movieId") UUID movieId,
            @Param("roomId") UUID roomId,
            @Param("startTimeFrom") LocalDateTime startTimeFrom,
            @Param("startTimeTo") LocalDateTime startTimeTo,
            @Param("status") ShowtimeStatus status,
            @Param("active") Boolean active
    );

    @Query("""
            select st from ShowTime st
            join fetch st.movie m
            join fetch st.room r
            join fetch r.cinema c
            where m.id = :movieId
              and m.active = true
              and st.active = true
              and st.status = :status
              and st.startTime >= :startTime
              and (:endTime is null or st.startTime < :endTime)
              and (:cinemaId is null or c.id = :cinemaId)
            order by st.startTime, c.name, r.name
            """)
    List<ShowTime> findBookableByMovie(
            @Param("movieId") UUID movieId,
            @Param("cinemaId") UUID cinemaId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("status") ShowtimeStatus status
    );

    @Query("""
        SELECT CASE WHEN COUNT(st) > 0 THEN true ELSE false END
        FROM ShowTime st
        WHERE st.room.id = :roomId
          AND st.active = true
          AND st.status <> :CANCELLED
          AND st.startTime < :newEndTime
          AND st.endTime > :newStartTime
        """)
    boolean existsOverlappingShowtime(
            @Param("roomId") UUID roomId,
            @Param("newStartTime") LocalDateTime newStartTime,
            @Param("newEndTime") LocalDateTime newEndTime,
            @Param("CANCELLED") ShowtimeStatus cancelledStatus
    );

    @Query("""
        SELECT CASE WHEN COUNT(st) > 0 THEN true ELSE false END
        FROM ShowTime st
        WHERE st.id <> :showtimeId
          AND st.room.id = :roomId
          AND st.active = true
          AND st.status <> :CANCELLED
          AND st.startTime < :newEndTime
          AND st.endTime > :newStartTime
        """)
    boolean existsOverlappingShowtimeExcludingId(
            @Param("showtimeId") UUID showtimeId,
            @Param("roomId") UUID roomId,
            @Param("newStartTime") LocalDateTime newStartTime,
            @Param("newEndTime") LocalDateTime newEndTime,
            @Param("CANCELLED") ShowtimeStatus cancelledStatus
    );
}
