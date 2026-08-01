package congtuong.dev.cinemabooking.ai.chat;

import congtuong.dev.cinemabooking.ai.chat.dto.ChatResponse;
import congtuong.dev.cinemabooking.ai.chat.exception.AiChatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiChatServiceImpl implements AiChatService {

    private static final String SYSTEM_PROMPT = """
            You are the CinemaBooking movie assistant.
            Answer in the same language as the user.
            Keep answers helpful, concise, and related to movies or cinemas.
            This phase has no access to CinemaBooking's movie, showtime, seat,
            price, booking, or payment data. Never claim that a movie is showing,
            a seat is available, or a price is current. Clearly say when live
            CinemaBooking data is required.
            """;

    private final ChatClient chatClient;

    @Override
    public ChatResponse chat(String message) {
        try {
            String answer = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user(message)
                    .call()
                    .content();

            if (answer == null || answer.isBlank()) {
                throw new AiChatException(
                        "AI provider returned an empty response"
                );
            }
            return new ChatResponse(answer);
        } catch (AiChatException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            log.error(
                    "OpenAI chat request failed: exceptionType={}, message={}",
                    exception.getClass().getName(),
                    exception.getMessage(),
                    exception
            );
            throw new AiChatException(
                    "AI assistant is temporarily unavailable",
                    exception
            );
        }
    }
}
