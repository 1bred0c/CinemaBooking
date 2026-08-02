package congtuong.dev.cinemabooking.ai.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.ai.rag")
public record MovieRagProperties(
        int topK,
        double similarityThreshold
) {
    public MovieRagProperties {
        if (topK <= 0) {
            throw new IllegalArgumentException("app.ai.rag.top-k must be positive");
        }
        if (similarityThreshold < 0 || similarityThreshold > 1) {
            throw new IllegalArgumentException(
                    "app.ai.rag.similarity-threshold must be between 0 and 1"
            );
        }
    }
}
