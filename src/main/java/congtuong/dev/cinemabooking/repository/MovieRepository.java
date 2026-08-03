package congtuong.dev.cinemabooking.repository;

import congtuong.dev.cinemabooking.entity.Movie;
import congtuong.dev.cinemabooking.entity.enums.ShowtimeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Optional;
import java.util.Collection;

public interface MovieRepository extends JpaRepository<Movie, UUID> {

    @EntityGraph(attributePaths = "genres")
    Optional<Movie> findWithGenresById(UUID id);

    @EntityGraph(attributePaths = "genres")
    List<Movie> findAllByActiveTrueOrderByTitleAsc();

    @EntityGraph(attributePaths = "genres")
    List<Movie> findAllByIdInAndActiveTrue(Collection<UUID> ids);

    @EntityGraph(attributePaths = "genres")
    List<Movie> findAllByIdIn(Collection<UUID> ids);

    @Query(
            value = """
            select m from Movie m
            where m.active = true
              and exists (
                  select st.id from ShowTime st
                  where st.movie = m
                    and st.active = true
                    and st.status = :status
                    and st.startTime > :now
              )
            """,
            countQuery = """
            select count(m) from Movie m
            where m.active = true
              and exists (
                  select st.id from ShowTime st
                  where st.movie = m
                    and st.active = true
                    and st.status = :status
                    and st.startTime > :now
              )
            """
    )
    Page<Movie> findNowShowing(
            @Param("status") ShowtimeStatus status,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );
}
