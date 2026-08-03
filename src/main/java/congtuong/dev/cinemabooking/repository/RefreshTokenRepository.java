package congtuong.dev.cinemabooking.repository;

import congtuong.dev.cinemabooking.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    List<RefreshToken> findByUserId(UUID userId);

    Optional<RefreshToken> findByToken(String token);

    @Modifying
    @Query("update RefreshToken rt set rt.revoked = true where rt.user.id = :userId and rt.revoked = false")
    int revokeAllByUserId(@Param("userId") UUID userId);

    @Modifying
    @Query("delete from RefreshToken rt where rt.revoked = true or rt.expiration <= :now")
    int deleteRevokedOrExpired(@Param("now") Instant now);
}
