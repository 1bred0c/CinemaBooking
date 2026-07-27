package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.GenreCreateRequest;
import congtuong.dev.cinemabooking.dto.request.GenreUpdateRequest;
import congtuong.dev.cinemabooking.dto.response.GenreResponse;
import congtuong.dev.cinemabooking.entity.Genre;
import congtuong.dev.cinemabooking.exception.GenreException;
import congtuong.dev.cinemabooking.repository.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GenreServiceImpl implements GenreService {
    private final GenreRepository genreRepository;

    @Override
    public List<GenreResponse> getGenres() {
        return genreRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    public GenreResponse getGenre(UUID id) {
        return toResponse(findGenre(id));
    }

    @Override
    @Transactional
    public GenreResponse createGenre(GenreCreateRequest request) {
        Genre genre = Genre.builder()
                .name(request.name())
                .description(request.description())
                .build();
        return toResponse(genreRepository.save(genre));
    }

    @Override
    @Transactional
    public GenreResponse updateGenre(UUID id, GenreUpdateRequest request) {
        Genre genre = findGenre(id);
        if (request.name() != null) genre.setName(request.name());
        if (request.description() != null) genre.setDescription(request.description());
        return toResponse(genre);
    }

    @Override
    @Transactional
    public void deactivateGenre(UUID id) {
        findGenre(id).setActive(false);
    }

    private Genre findGenre(UUID id) {
        return genreRepository.findById(id)
                .orElseThrow(() -> new GenreException("Genre not found"));
    }

    private GenreResponse toResponse(Genre genre) {
        return new GenreResponse(genre.getId(), genre.getName(), genre.getDescription(),
                genre.isActive(), genre.getCreatedAt(), genre.getUpdatedAt());
    }
}
