package congtuong.dev.cinemabooking.ai.indexing;

import congtuong.dev.cinemabooking.entity.Genre;
import congtuong.dev.cinemabooking.entity.Movie;
import congtuong.dev.cinemabooking.entity.enums.AgeRating;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovieDocumentFactoryTest {

    private final MovieDocumentFactory factory = new MovieDocumentFactory();

    @Test
    void createsGroundingContentAndStableMetadataFromMovie() {
        UUID movieId = UUID.randomUUID();
        Movie movie = Movie.builder()
                .id(movieId)
                .title("Interstellar")
                .description("Explorers travel through a wormhole in space.")
                .director("Christopher Nolan")
                .durationMinutes(169)
                .releaseDate(LocalDate.of(2014, 11, 7))
                .ageRating(AgeRating.T13)
                .active(true)
                .genres(new LinkedHashSet<>())
                .build();
        movie.getGenres().add(Genre.builder().name("Science Fiction").build());

        Document document = factory.create(movie);

        assertEquals(movieId.toString(), document.getId());
        assertTrue(document.getText().contains("Movie title: Interstellar"));
        assertTrue(document.getText().contains("Genres: Science Fiction"));
        assertEquals(movieId.toString(), document.getMetadata().get("movieId"));
        assertEquals(true, document.getMetadata().get("active"));
    }
}
