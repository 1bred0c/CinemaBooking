package congtuong.dev.cinemabooking.repository;

import congtuong.dev.cinemabooking.entity.Cinema;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
import java.util.List;

@Repository
public interface CinemaRepository extends JpaRepository<Cinema, UUID> {
    List<Cinema> findAllByIsActiveTrueOrderByName();
}
