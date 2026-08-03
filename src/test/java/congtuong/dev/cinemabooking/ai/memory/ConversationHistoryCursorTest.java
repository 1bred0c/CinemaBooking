package congtuong.dev.cinemabooking.ai.memory;

import congtuong.dev.cinemabooking.repository.AiChatMessageRepository;
import congtuong.dev.cinemabooking.repository.AiConversationRepository;
import congtuong.dev.cinemabooking.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationHistoryCursorTest {
    @Mock private AiConversationRepository conversationRepository;
    @Mock private AiChatMessageRepository messageRepository;
    @Mock private UserRepository userRepository;

    @Test
    void historyReturnsChronologicalPageAndCursorForOlderMessages() {
        UUID userId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        AiConversation conversation = mock(AiConversation.class);
        when(conversation.getId()).thenReturn(conversationId);
        when(conversationRepository.findByUserId(userId))
                .thenReturn(Optional.of(conversation));
        when(messageRepository
                .findByConversationIdAndSequenceLessThanOrderBySequenceDesc(
                        eq(conversationId),
                        eq(10L),
                        argThat((Pageable pageable) -> pageable.getPageSize() == 3)
                ))
                .thenReturn(List.of(
                        message(conversation, 9L, "newest"),
                        message(conversation, 8L, "older"),
                        message(conversation, 7L, "look-ahead")
                ));
        ConversationMemoryPersistenceService service =
                new ConversationMemoryPersistenceService(
                        conversationRepository,
                        messageRepository,
                        userRepository,
                        new ConversationMemoryProperties(5, 5)
                );

        var response = service.getHistory(userId, 10L, 2);

        assertThat(response.messages())
                .extracting(message -> message.content())
                .containsExactly("older", "newest");
        assertThat(response.hasMore()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(8L);
    }

    private AiChatMessage message(
            AiConversation conversation,
            long sequence,
            String content
    ) {
        return AiChatMessage.create(
                conversation,
                ChatMessageRole.USER,
                sequence,
                content,
                Instant.now()
        );
    }
}
