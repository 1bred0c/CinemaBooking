package congtuong.dev.cinemabooking.ai.memory;

import java.util.UUID;

public record ConversationSummaryWork(
        UUID conversationId,
        String currentSummary,
        String newTranscript,
        long throughTurns,
        long throughSequence
) {
}
