package congtuong.dev.cinemabooking.ai.memory;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.memory")
public record ConversationMemoryProperties(
        int recentTurns,
        int summaryIntervalTurns
) {
    public ConversationMemoryProperties {
        if (recentTurns <= 0) recentTurns = 5;
        if (summaryIntervalTurns <= 0) summaryIntervalTurns = 5;
    }
}
