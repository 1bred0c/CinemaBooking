package congtuong.dev.cinemabooking.ai.memory;

import congtuong.dev.cinemabooking.entity.User;
import congtuong.dev.cinemabooking.ai.memory.dto.ChatHistoryMessageResponse;
import congtuong.dev.cinemabooking.ai.memory.dto.ChatHistoryResponse;
import congtuong.dev.cinemabooking.repository.AiChatMessageRepository;
import congtuong.dev.cinemabooking.repository.AiConversationRepository;
import congtuong.dev.cinemabooking.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConversationMemoryPersistenceService {

    private final AiConversationRepository conversationRepository;
    private final AiChatMessageRepository messageRepository;
    private final UserRepository userRepository;
    private final ConversationMemoryProperties properties;

    public ConversationMemoryContext load(UUID userId) {
        return conversationRepository.findByUserId(userId)
                .map(this::toContext)
                .orElseGet(ConversationMemoryContext::empty);
    }

    public ChatHistoryResponse getHistory(UUID userId) {
        return conversationRepository.findByUserId(userId)
                .map(conversation -> new ChatHistoryResponse(
                        conversation.getId(),
                        messageRepository
                                .findByConversationIdOrderBySequence(
                                        conversation.getId()
                                )
                                .stream()
                                .map(message -> new ChatHistoryMessageResponse(
                                        message.getId(),
                                        message.getRole(),
                                        message.getContent(),
                                        message.getCreatedAt()
                                ))
                                .toList()
                ))
                .orElseGet(ChatHistoryResponse::empty);
    }

    @Transactional
    public void recordExchange(UUID userId, String userMessage, String answer) {
        Instant now = Instant.now();
        AiConversation conversation = conversationRepository
                .findByUserIdForUpdate(userId)
                .orElseGet(() -> createConversation(userId, now));
        messageRepository.save(AiChatMessage.create(
                conversation,
                ChatMessageRole.USER,
                conversation.takeNextSequence(now),
                userMessage,
                now
        ));
        messageRepository.save(AiChatMessage.create(
                conversation,
                ChatMessageRole.ASSISTANT,
                conversation.takeNextSequence(now),
                answer,
                now
        ));
        conversation.completeTurn(now);
    }

    public Optional<ConversationSummaryWork> findSummaryWork(UUID userId) {
        return conversationRepository.findByUserId(userId)
                .filter(conversation -> conversation.needsSummary(
                        properties.summaryIntervalTurns()
                ))
                .map(conversation -> {
                    List<AiChatMessage> messages = messageRepository
                            .findByConversationIdAndSequenceGreaterThanOrderBySequence(
                                    conversation.getId(),
                                    conversation.getSummarizedThroughSequence()
                            );
                    long throughSequence = messages.stream()
                            .mapToLong(AiChatMessage::getSequence)
                            .max()
                            .orElse(conversation.getSummarizedThroughSequence());
                    return new ConversationSummaryWork(
                            conversation.getId(),
                            conversation.getRollingSummary(),
                            transcript(messages),
                            conversation.getCompletedTurns(),
                            throughSequence
                    );
                });
    }

    @Transactional
    public void saveSummary(ConversationSummaryWork work, String summary) {
        AiConversation conversation = conversationRepository
                .findById(work.conversationId())
                .orElseThrow();
        if (work.throughTurns() > conversation.getSummarizedTurns()) {
            conversation.updateSummary(
                    summary,
                    work.throughTurns(),
                    work.throughSequence(),
                    Instant.now()
            );
        }
    }

    private ConversationMemoryContext toContext(AiConversation conversation) {
        List<AiChatMessage> recent = new ArrayList<>(messageRepository
                .findByConversationIdOrderBySequenceDesc(
                        conversation.getId(),
                        PageRequest.of(0, properties.recentTurns() * 2)
                ));
        Collections.reverse(recent);
        return new ConversationMemoryContext(
                conversation.getRollingSummary(),
                transcript(recent)
        );
    }

    private AiConversation createConversation(UUID userId, Instant now) {
        User user = userRepository.findById(userId).orElseThrow();
        return conversationRepository.save(AiConversation.create(user, now));
    }

    private String transcript(List<AiChatMessage> messages) {
        return messages.stream()
                .map(message -> message.getRole() + ": " + message.getContent())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }
}
