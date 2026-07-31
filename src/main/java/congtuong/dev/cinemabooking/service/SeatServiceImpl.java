package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.SeatCreateRequest;
import congtuong.dev.cinemabooking.dto.request.SeatLayoutCreateRequest;
import congtuong.dev.cinemabooking.dto.request.SeatRowCreateRequest;
import congtuong.dev.cinemabooking.dto.request.SeatSectionCreateRequest;
import congtuong.dev.cinemabooking.dto.request.SeatUpdateRequest;
import congtuong.dev.cinemabooking.dto.response.SeatResponse;
import congtuong.dev.cinemabooking.entity.Room;
import congtuong.dev.cinemabooking.entity.Seat;
import congtuong.dev.cinemabooking.entity.enums.RoomStatus;
import congtuong.dev.cinemabooking.exception.SeatException;
import congtuong.dev.cinemabooking.repository.RoomRepository;
import congtuong.dev.cinemabooking.repository.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
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
        Room room = findActiveRoom(request.roomId());
        String row = normalizeRow(request.row());
        validateSeatNumber(request.number());
        ensurePositionAvailable(room.getId(), row, request.number(), null);
        validateRowCapacity(
                room,
                seatRepository.findByRoomId(room.getId()),
                Set.of(row),
                null
        );

        Seat seat = Seat.builder()
                .room(room)
                .row(row)
                .number(request.number())
                .type(request.type())
                .build();

        return toSeatResponse(seatRepository.save(seat));
    }

    @Override
    @Transactional
    public List<SeatResponse> createSeatLayout(
            SeatLayoutCreateRequest request
    ) {
        Room room = findActiveRoom(request.roomId());
        List<Seat> existingSeats = seatRepository.findByRoomId(room.getId());
        Set<SeatPosition> occupiedPositions = existingSeats.stream()
                .map(seat -> new SeatPosition(
                        normalizeRow(seat.getRow()),
                        seat.getNumber()
                ))
                .collect(java.util.stream.Collectors.toSet());
        Set<SeatPosition> requestedPositions = new HashSet<>();
        List<Seat> newSeats = new ArrayList<>();

        for (SeatRowCreateRequest rowRequest : request.rows()) {
            String row = normalizeRow(rowRequest.row());

            for (SeatSectionCreateRequest section : rowRequest.sections()) {
                validateSection(section);

                for (int number = section.startNumber();
                     number <= section.endNumber();
                     number++) {
                    SeatPosition position = new SeatPosition(row, number);
                    if (!requestedPositions.add(position)) {
                        throw new SeatException(
                                "Seat " + row + number
                                        + " is duplicated in the requested layout"
                        );
                    }
                    if (occupiedPositions.contains(position)) {
                        throw new SeatException(
                                "Seat " + row + number
                                        + " already exists in this room"
                        );
                    }

                    newSeats.add(
                            Seat.builder()
                                    .room(room)
                                    .row(row)
                                    .number(number)
                                    .type(section.type())
                                    .build()
                    );
                }
            }
        }

        validateRowCapacity(
                room,
                existingSeats,
                newSeats.stream()
                        .map(Seat::getRow)
                        .collect(java.util.stream.Collectors.toSet()),
                null
        );

        return seatRepository.saveAll(newSeats)
                .stream()
                .map(this::toSeatResponse)
                .toList();
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
    }

    @Override
    @Transactional
    public SeatResponse updateSeat(UUID seatId, SeatUpdateRequest request) {
        Seat seat = findSeat(seatId);
        UUID targetRoomId = request.roomId() == null
                ? seat.getRoom().getId()
                : request.roomId();
        Room targetRoom = findActiveRoom(targetRoomId);
        String targetRow = request.row() == null
                ? normalizeRow(seat.getRow())
                : normalizeRow(request.row());
        Integer targetNumber = request.number() == null
                ? seat.getNumber()
                : request.number();

        validateSeatNumber(targetNumber);
        ensurePositionAvailable(
                targetRoom.getId(),
                targetRow,
                targetNumber,
                seat.getId()
        );
        validateRowCapacity(
                targetRoom,
                seatRepository.findByRoomId(targetRoom.getId()),
                Set.of(targetRow),
                seat.getId()
        );

        seat.setRoom(targetRoom);
        seat.setRow(targetRow);
        seat.setNumber(targetNumber);
        if (request.type() != null) {
            seat.setType(request.type());
        }

        return toSeatResponse(seat);
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

    private Room findActiveRoom(UUID roomId) {
        return roomRepository.findByIdForUpdate(roomId, RoomStatus.ACTIVE)
                .orElseThrow(() -> new SeatException(
                        "Active room not found"
                ));
    }

    private String normalizeRow(String row) {
        if (row == null || row.isBlank()) {
            throw new SeatException("Seat row is required");
        }
        return row.trim().toUpperCase(Locale.ROOT);
    }

    private void validateSection(SeatSectionCreateRequest section) {
        if (section.startNumber() > section.endNumber()) {
            throw new SeatException(
                    "Seat section start number must not exceed end number"
            );
        }
        validateSeatNumber(section.endNumber());
    }

    private void validateSeatNumber(Integer number) {
        if (number == null || number < 1) {
            throw new SeatException("Seat number must be positive");
        }
    }

    private void ensurePositionAvailable(
            UUID roomId,
            String row,
            Integer number,
            UUID ignoredSeatId
    ) {
        seatRepository.findByRoomIdAndNumberAndRow(roomId, number, row)
                .filter(existing -> !existing.getId().equals(ignoredSeatId))
                .ifPresent(existing -> {
                    throw new SeatException(
                            "Seat " + row + number
                                    + " already exists in this room"
                    );
                });
    }

    private void validateRowCapacity(
            Room room,
            List<Seat> existingSeats,
            Set<String> requestedRows,
            UUID ignoredSeatId
    ) {
        Set<String> rows = new HashSet<>();
        existingSeats.stream()
                .filter(Seat::isActive)
                .filter(seat -> !Objects.equals(
                        seat.getId(),
                        ignoredSeatId
                ))
                .map(Seat::getRow)
                .map(this::normalizeRow)
                .forEach(rows::add);
        requestedRows.stream()
                .map(this::normalizeRow)
                .forEach(rows::add);

        if (room.getTotalRows() != null
                && rows.size() > room.getTotalRows()) {
            throw new SeatException("Room row capacity would be exceeded");
        }
    }

    private record SeatPosition(String row, Integer number) {
    }
}
