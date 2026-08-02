package congtuong.dev.cinemabooking.ai.indexing;

import congtuong.dev.cinemabooking.entity.Genre;
import congtuong.dev.cinemabooking.entity.Movie;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class MovieDocumentFactory {

    public Document create(Movie movie) {
        String genres = movie.getGenres().stream()
                .map(Genre::getName)
                .sorted()
                .collect(Collectors.joining(", "));

        String content = """
                Movie title: %s
                Description: %s
                Genres: %s
                Director: %s
                Duration: %s minutes
                Release date: %s
                Age rating: %s
                """.formatted(
                value(movie.getTitle()),
                value(movie.getDescription()),
                value(genres),
                value(movie.getDirector()),
                value(movie.getDurationMinutes()),
                value(movie.getReleaseDate()),
                value(movie.getAgeRating())
        ).strip();

        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("movieId", movie.getId().toString());
        metadata.put("title", movie.getTitle());
        metadata.put("active", movie.isActive());
        metadata.put("sourceType", "MOVIE");
        metadata.put("genres", genres);
        if (movie.getAgeRating() != null) {
            metadata.put("ageRating", movie.getAgeRating().name());
        }

        return Document.builder()
                .id(documentId(movie))
                .text(content)
                .metadata(metadata)
                .build();
    }

    public String documentId(Movie movie) {
        return movie.getId().toString();
    }

    private String value(Object value) {
        return value == null || value.toString().isBlank()
                ? "Not provided"
                : value.toString();
    }
}
