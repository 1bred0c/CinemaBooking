package congtuong.dev.cinemabooking;

import congtuong.dev.cinemabooking.config.DotEnvOpenAiKeyLoader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class CinemaBookingApplication {

    public static void main(String[] args) {
        DotEnvOpenAiKeyLoader.loadFromWorkingDirectory();
        SpringApplication.run(CinemaBookingApplication.class, args);
    }

}
