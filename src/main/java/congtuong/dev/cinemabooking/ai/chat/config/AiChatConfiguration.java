package congtuong.dev.cinemabooking.ai.chat.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiChatConfiguration {

    @Bean
    ChatClient cinemaChatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
