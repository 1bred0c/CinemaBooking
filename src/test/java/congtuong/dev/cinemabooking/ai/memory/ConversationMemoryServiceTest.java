package congtuong.dev.cinemabooking.ai.memory;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationMemoryServiceTest {

    @Mock private ConversationMemoryPersistenceService persistenceService;
    @Mock private ChatClient chatClient;
    @Mock private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock private ChatClient.CallResponseSpec responseSpec;

    @Test
    void recordsExchangeWithoutSummarizingBeforeThreshold() {
        UUID userId = UUID.randomUUID();
        when(persistenceService.findSummaryWork(userId))
                .thenReturn(Optional.empty());
        ConversationMemoryService service = service();

        service.recordSuccessfulExchange(userId, "hello", "hi");

        verify(persistenceService).recordExchange(userId, "hello", "hi");
        verify(chatClient, never()).prompt();
    }

    @Test
    void updatesRollingSummaryWhenWorkIsAvailable() {
        UUID userId = UUID.randomUUID();
        ConversationSummaryWork work = new ConversationSummaryWork(
                UUID.randomUUID(),
                "User likes action movies",
                "USER: tomorrow?\nASSISTANT: I will check",
                5,
                10
        );
        when(persistenceService.findSummaryWork(userId))
                .thenReturn(Optional.of(work));
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(responseSpec);
        when(responseSpec.content()).thenReturn("Updated compact summary");
        ConversationMemoryService service = service();

        service.recordSuccessfulExchange(userId, "question", "answer");

        verify(persistenceService).saveSummary(
                work,
                "Updated compact summary"
        );
    }

    private ConversationMemoryService service() {
        return new ConversationMemoryService(persistenceService, chatClient);
    }
}
