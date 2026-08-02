package congtuong.dev.cinemabooking.ai.memory;

import congtuong.dev.cinemabooking.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ai_conversations")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiConversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "rolling_summary", columnDefinition = "text")
    private String rollingSummary;

    @Column(name = "next_sequence", nullable = false)
    private long nextSequence;

    @Column(name = "completed_turns", nullable = false)
    private long completedTurns;

    @Column(name = "summarized_turns", nullable = false)
    private long summarizedTurns;

    @Column(name = "summarized_through_sequence", nullable = false)
    private long summarizedThroughSequence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    public static AiConversation create(User user, Instant now) {
        AiConversation conversation = new AiConversation();
        conversation.user = user;
        conversation.nextSequence = 1;
        conversation.createdAt = now;
        conversation.updatedAt = now;
        return conversation;
    }

    public long takeNextSequence(Instant now) {
        long sequence = nextSequence++;
        updatedAt = now;
        return sequence;
    }

    public void completeTurn(Instant now) {
        completedTurns++;
        updatedAt = now;
    }

    public boolean needsSummary(int intervalTurns) {
        return completedTurns - summarizedTurns >= intervalTurns;
    }

    public void updateSummary(
            String summary,
            long throughTurns,
            long throughSequence,
            Instant now
    ) {
        rollingSummary = summary;
        summarizedTurns = throughTurns;
        summarizedThroughSequence = throughSequence;
        updatedAt = now;
    }
}
