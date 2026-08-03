package congtuong.dev.cinemabooking.repository;

import congtuong.dev.cinemabooking.ai.memory.AiChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiChatMessageRepository
        extends JpaRepository<AiChatMessage, UUID> {

    List<AiChatMessage> findByConversationIdOrderBySequenceDesc(
            UUID conversationId,
            Pageable pageable
    );

    List<AiChatMessage> findByConversationIdAndSequenceGreaterThanOrderBySequence(
            UUID conversationId,
            long sequence
    );

    List<AiChatMessage> findByConversationIdAndSequenceLessThanOrderBySequenceDesc(
            UUID conversationId,
            long sequence,
            Pageable pageable
    );
}
