package congtuong.dev.cinemabooking.repository;

import congtuong.dev.cinemabooking.ai.memory.AiConversation;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface AiConversationRepository
        extends JpaRepository<AiConversation, UUID> {

    Optional<AiConversation> findByUserId(UUID userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AiConversation c where c.user.id = :userId")
    Optional<AiConversation> findByUserIdForUpdate(@Param("userId") UUID userId);
}
