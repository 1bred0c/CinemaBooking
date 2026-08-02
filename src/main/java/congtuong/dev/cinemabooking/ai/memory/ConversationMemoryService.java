package congtuong.dev.cinemabooking.ai.memory;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.UUID;
import congtuong.dev.cinemabooking.ai.memory.dto.ChatHistoryResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConversationMemoryService {

    private static final String SUMMARY_PROMPT = """
            Maintain a compact rolling summary for a movie-booking assistant.
            Preserve explicit user preferences, referenced movies/cinemas,
            unresolved requests and decisions needed for future follow-ups.
            Never present old showtimes, prices, or seat counts as current facts;
            say they must be checked again. Do not preserve greetings, debug
            details, retrieval scores, or verbose assistant prose.
            Return only the updated summary in the user's language.
            """;

    private final ConversationMemoryPersistenceService persistenceService;
    private final ChatClient chatClient;

    public ConversationMemoryContext load(UUID userId) {
        return persistenceService.load(userId);
    }

    public ChatHistoryResponse getHistory(UUID userId) {
        return persistenceService.getHistory(userId);
    }

    public void recordSuccessfulExchange(
            UUID userId,
            String userMessage,
            String answer
    ) {
        persistenceService.recordExchange(userId, userMessage, answer);
        updateSummaryIfNeeded(userId);
    }

    private void updateSummaryIfNeeded(UUID userId) {
        persistenceService.findSummaryWork(userId).ifPresent(work -> {
            try {
                String summary = chatClient.prompt()
                        .system(SUMMARY_PROMPT)
                        .user("""
                                CURRENT SUMMARY:
                                %s

                                NEW COMPLETED TURNS:
                                %s
                                """.formatted(
                                work.currentSummary() == null
                                        ? "(none)"
                                        : work.currentSummary(),
                                work.newTranscript()
                        ))
                        .call()
                        .content();
                if (summary != null && !summary.isBlank()) {
                    persistenceService.saveSummary(work, summary.trim());
                }
            } catch (RuntimeException exception) {
                log.warn(
                        "Conversation summary update failed for user {}: {}",
                        userId,
                        exception.getMessage()
                );
            }
        });
    }
}
