package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.RoomCreateRequest;
import congtuong.dev.cinemabooking.dto.response.RoomResponse;
import congtuong.dev.cinemabooking.dto.request.RoomUpdateRequest;
import congtuong.dev.cinemabooking.entity.Room;
import congtuong.dev.cinemabooking.entity.enums.RoomStatus;
import congtuong.dev.cinemabooking.exception.RoomException;
import congtuong.dev.cinemabooking.repository.CinemaRepository;
import congtuong.dev.cinemabooking.repository.RoomRepository;
import congtuong.dev.cinemabooking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly=true)
@RequiredArgsConstructor
public class RoomServiceImpl implements RoomService {

    private final RoomRepository roomRepository;
    private final CinemaRepository cinemaRepository;
    private final SeatRepository seatRepository;

    @Override
    public List<RoomResponse> findAllRooms() {
        return roomRepository.findAll().stream().map(this::toRoomResponse).toList();
    }

    @Override
    @Transactional
    public RoomResponse createRoom(RoomCreateRequest roomCreateRequest) {
        Room room = roomRepository.save(
                Room.builder()
                .name(roomCreateRequest.name())
                .totalRows(roomCreateRequest.totalRows())
                        .cinema(cinemaRepository.findById(roomCreateRequest.cinemaId()).orElseThrow(
                                () -> new RoomException("Cinema not found")))
                .roomType(roomCreateRequest.roomType())
                .status(RoomStatus.ACTIVE)
                .build()
        );
        return toRoomResponse(room);
    }

    @Override
    public RoomResponse getRoom(UUID roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(
                () -> new RoomException("Room not found")
        );
        return toRoomResponse(room);
    }

    @Override
    @Transactional
    public RoomResponse updateRoom(UUID roomId, RoomUpdateRequest roomUpdateRequest) {
        Room room = roomRepository.findById(roomId).orElseThrow(
                () -> new RoomException("Room not found")
        );
        // Update room properties based on roomUpdateRequest
        if (roomUpdateRequest.name() != null) {
            room.setName(roomUpdateRequest.name());
        }
        if (roomUpdateRequest.roomType() != null) {
            room.setRoomType(roomUpdateRequest.roomType());
        }
        if (roomUpdateRequest.roomStatus() != null) {
            room.setStatus(roomUpdateRequest.roomStatus());
        }
        if (roomUpdateRequest.totalRows() != null) {
            room.setTotalRows(roomUpdateRequest.totalRows());
        }

        if (roomUpdateRequest.cinemaId() != null) {
            room.setCinema(cinemaRepository.findById(roomUpdateRequest.cinemaId()).orElseThrow(
                    () -> new RoomException("Cinema not found")
            ));
        }
        return toRoomResponse(room);
    }

    @Override
    @Transactional
    public RoomResponse deactivateRoom(UUID roomId) {
        Room room = roomRepository.findById(roomId).orElseThrow(
                () -> new RoomException("Room not found")
        );
        room.setStatus(RoomStatus.INACTIVE);
        return toRoomResponse(room);
    }

    private RoomResponse toRoomResponse(Room room){
        return new RoomResponse(
                room.getId(),
                room.getName(),
                Math.toIntExact(
                        seatRepository.countByRoomIdAndIsActiveTrue(
                                room.getId()
                        )
                ),
                room.getTotalRows(),
                room.getRoomType(),
                room.getStatus(),
                room.getCinema().getId()
        );
    }
}
