package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.SeatCreateRequest;
import congtuong.dev.cinemabooking.dto.request.SeatUpdateRequest;
import congtuong.dev.cinemabooking.dto.response.SeatResponse;
import congtuong.dev.cinemabooking.entity.Room;
import congtuong.dev.cinemabooking.entity.Seat;
import congtuong.dev.cinemabooking.exception.SeatException;
import congtuong.dev.cinemabooking.repository.RoomRepository;
import congtuong.dev.cinemabooking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SeatServiceImpl implements SeatService {

    private final SeatRepository seatRepository;
    private final RoomRepository roomRepository;

    @Override
    @Transactional
    public SeatResponse createSeat(SeatCreateRequest request) {
        Room room = roomRepository.findById(request.roomId()).orElseThrow(
                () -> new SeatException("Room not found")
        );

        Seat seat = Seat.builder()
                .room(room)
                .row(request.row())
                .number(request.number())
                .type(request.type())
                .build();

        return toSeatResponse(seatRepository.save(seat));
    }

    @Override
    public List<SeatResponse> getAllSeats() {
        return seatRepository.findAll()
                .stream()
                .map(this::toSeatResponse)
                .toList();
    }

    @Override
    public SeatResponse getSeatById(UUID seatId) {
        Seat seat = findSeat(seatId);
        return toSeatResponse(seat);
    }

    @Override
    @Transactional
    public void deactivateSeat(UUID seatId) {
        Seat seat = findSeat(seatId);
        seat.setActive(false);
        seatRepository.save(seat);
    }

    @Override
    @Transactional
    public SeatResponse updateSeat(UUID seatId, SeatUpdateRequest request) {
        Seat seat = findSeat(seatId);

        if (request.roomId() != null) {
            Room room = roomRepository.findById(request.roomId()).orElseThrow(
                    () -> new SeatException("Room not found")
            );
            seat.setRoom(room);
        }
        if (request.row() != null) {
            seat.setRow(request.row());
        }
        if (request.number() != null) {
            seat.setNumber(request.number());
        }
        if (request.type() != null) {
            seat.setType(request.type());
        }

        return toSeatResponse(seatRepository.save(seat));
    }

    @Override
    public List<SeatResponse> getSeatsByRoomId(UUID roomId) {
        return seatRepository.findByRoomId(roomId)
                .stream()
                .map(this::toSeatResponse)
                .toList();
    }

    private SeatResponse toSeatResponse(Seat seat) {
        return new SeatResponse(
                seat.getId(),
                seat.getRoom().getId(),
                seat.getRoom().getName(),
                seat.getRow(),
                seat.getNumber(),
                seat.getType(),
                seat.isActive()
        );
    }

    private Seat findSeat(UUID seatId) {
        return seatRepository.findById(seatId).orElseThrow(
                () -> new SeatException("Seat not found")
        );
    }
}
