package congtuong.dev.cinemabooking.repository;

import congtuong.dev.cinemabooking.entity.Genre;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GenreRepository extends JpaRepository<Genre, UUID> {
}
