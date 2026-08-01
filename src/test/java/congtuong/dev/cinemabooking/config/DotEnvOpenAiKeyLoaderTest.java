package congtuong.dev.cinemabooking.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DotEnvOpenAiKeyLoaderTest {

    @TempDir
    private Path tempDir;

    @AfterEach
    void clearLoadedProperty() {
        System.clearProperty(
                DotEnvOpenAiKeyLoader.OPENAI_PROPERTY_NAME
        );
    }

    @Test
    void loadsKeyFromDotEnvIntoCanonicalSpringProperty()
            throws IOException {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "OPENAI_API_KEY='test-secret'\n");

        DotEnvOpenAiKeyLoader.load(envFile);

        assertEquals(
                "test-secret",
                System.getProperty(
                        DotEnvOpenAiKeyLoader.OPENAI_PROPERTY_NAME
                )
        );
    }

    @Test
    void fileValueOverridesExistingProcessDerivedProperty()
            throws IOException {
        System.setProperty(
                DotEnvOpenAiKeyLoader.OPENAI_PROPERTY_NAME,
                "stale-key"
        );
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "OPENAI_API_KEY=fresh-key\n");

        DotEnvOpenAiKeyLoader.load(envFile);

        assertEquals(
                "fresh-key",
                System.getProperty(
                        DotEnvOpenAiKeyLoader.OPENAI_PROPERTY_NAME
                )
        );
    }

    @Test
    void rejectsMissingKey() throws IOException {
        Path envFile = tempDir.resolve(".env");
        Files.writeString(envFile, "DB_URL=jdbc:postgresql://localhost/db\n");

        assertThrows(
                IllegalStateException.class,
                () -> DotEnvOpenAiKeyLoader.load(envFile)
        );
    }
}
