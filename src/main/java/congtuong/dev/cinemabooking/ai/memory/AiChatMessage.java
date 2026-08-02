package congtuong.dev.cinemabooking.ai.memory;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "ai_chat_messages",
        indexes = @Index(
                name = "idx_ai_chat_message_conversation_sequence",
                columnList = "conversation_id, sequence_number"
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "conversation_id", nullable = false)
    private AiConversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ChatMessageRole role;

    @Column(name = "sequence_number", nullable = false)
    private long sequence;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public static AiChatMessage create(
            AiConversation conversation,
            ChatMessageRole role,
            long sequence,
            String content,
            Instant now
    ) {
        AiChatMessage message = new AiChatMessage();
        message.conversation = conversation;
        message.role = role;
        message.sequence = sequence;
        message.content = content;
        message.createdAt = now;
        return message;
    }
}
