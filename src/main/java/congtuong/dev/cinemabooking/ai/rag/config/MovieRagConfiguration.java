package congtuong.dev.cinemabooking.ai.rag.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MovieRagProperties.class)
public class MovieRagConfiguration {
}
