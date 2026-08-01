package congtuong.dev.cinemabooking.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class DotEnvOpenAiKeyLoader {

    static final String OPENAI_ENV_NAME = "OPENAI_API_KEY";
    static final String OPENAI_PROPERTY_NAME =
            "spring.ai.openai.api-key";

    private DotEnvOpenAiKeyLoader() {
    }

    public static void loadFromWorkingDirectory() {
        load(Path.of(System.getProperty("user.dir"), ".env"));
    }

    static void load(Path envFile) {
        if (!Files.isRegularFile(envFile)) {
            throw new IllegalStateException(
                    ".env file not found in working directory: "
                            + envFile.toAbsolutePath()
            );
        }

        String apiKey = readApiKey(envFile);
        System.setProperty(OPENAI_PROPERTY_NAME, apiKey);
    }

    private static String readApiKey(Path envFile) {
        try {
            List<String> matchingLines = Files.readAllLines(envFile)
                    .stream()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .filter(line -> !line.startsWith("#"))
                    .filter(line -> line.startsWith(OPENAI_ENV_NAME + "="))
                    .toList();

            if (matchingLines.size() != 1) {
                throw new IllegalStateException(
                        ".env must contain exactly one OPENAI_API_KEY entry"
                );
            }

            String value = matchingLines.get(0)
                    .substring((OPENAI_ENV_NAME + "=").length())
                    .trim();
            value = removeMatchingQuotes(value);

            if (value.isBlank()) {
                throw new IllegalStateException(
                        "OPENAI_API_KEY in .env must not be blank"
                );
            }
            return value;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Unable to read .env from working directory",
                    exception
            );
        }
    }

    private static String removeMatchingQuotes(String value) {
        if (value.length() < 2) {
            return value;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"')
                || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length() - 1).trim();
        }
        return value;
    }
}
