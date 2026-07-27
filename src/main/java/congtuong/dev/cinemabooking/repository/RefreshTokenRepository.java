package congtuong.dev.cinemabooking.repository;

import congtuong.dev.cinemabooking.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    List<RefreshToken> findByUserId(UUID userId);

    Optional<RefreshToken> findByToken(String token);
}
