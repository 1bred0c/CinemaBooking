package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.CinemaCreateRequest;
import congtuong.dev.cinemabooking.dto.response.CinemaResponse;
import congtuong.dev.cinemabooking.dto.request.CinemaUpdateRequest;
import congtuong.dev.cinemabooking.entity.Cinema;
import congtuong.dev.cinemabooking.exception.CinemaNotFoundException;
import congtuong.dev.cinemabooking.repository.CinemaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CinemaServiceImpl implements CinemaService {

    private final CinemaRepository cinemaRepository;

    @Override
    public List<CinemaResponse> getCinemas() {
        return cinemaRepository.findAll()
                .stream()
                .map(this::toCinemaResponse)
                .toList();
    }

    @Override
    public List<CinemaResponse> getActiveCinemas() {
        return cinemaRepository.findAllByIsActiveTrueOrderByName()
                .stream()
                .map(this::toCinemaResponse)
                .toList();
    }

    @Override
    public CinemaResponse getCinema(UUID id) {
        Cinema cinema = findCinema(id);
        return toCinemaResponse(cinema);
    }

    @Override
    @Transactional
    public CinemaResponse createCinema(CinemaCreateRequest request) {
        Cinema cinema = Cinema.builder()
                .name(request.name())
                .address(request.address())
                .build();
        return toCinemaResponse(cinemaRepository.save(cinema));
    }

    @Override
    @Transactional
    public CinemaResponse updateCinema(UUID id, CinemaUpdateRequest request) {
        Cinema cinema = findCinema(id);
        if (request.name() != null) {
            cinema.setName(request.name());
        }
        if (request.address() != null) {
            cinema.setAddress(request.address());
        }
        return toCinemaResponse(cinemaRepository.save(cinema));
    }

    @Override
    @Transactional
    public CinemaResponse deactivateCinema(UUID id) {
        Cinema existingCinema = findCinema(id);
        existingCinema.setActive(false);
        return toCinemaResponse(cinemaRepository.save(existingCinema));
    }

    @Override
    public CinemaResponse activateCinema(UUID id) {
        Cinema existingCinema = findCinema(id);
        existingCinema.setActive(true);
        return toCinemaResponse(cinemaRepository.save(existingCinema));
    }

    private CinemaResponse toCinemaResponse(Cinema cinema) {
        return new CinemaResponse(
                cinema.getId(),
                cinema.getName(),
                cinema.getAddress(),
                cinema.isActive()
        );
    }

    private Cinema findCinema(UUID id) {
        return cinemaRepository.findById(id).orElseThrow(
                () -> new CinemaNotFoundException("Cinema not found")
        );
    }
}
