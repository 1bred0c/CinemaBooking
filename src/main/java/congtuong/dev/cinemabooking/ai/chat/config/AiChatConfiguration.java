package congtuong.dev.cinemabooking.ai.chat.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import congtuong.dev.cinemabooking.ai.memory.ConversationMemoryProperties;

@Configuration
@EnableConfigurationProperties(ConversationMemoryProperties.class)
public class AiChatConfiguration {

    @Bean
    ChatClient cinemaChatClient(ChatClient.Builder builder) {
        return builder.build();
    }
}
