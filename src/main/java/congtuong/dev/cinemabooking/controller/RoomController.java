package congtuong.dev.cinemabooking.controller;

import congtuong.dev.cinemabooking.dto.request.RoomCreateRequest;
import congtuong.dev.cinemabooking.dto.response.RoomResponse;
import congtuong.dev.cinemabooking.dto.request.RoomUpdateRequest;
import congtuong.dev.cinemabooking.service.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/room")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<List<RoomResponse>> findAllRooms(){
        return ResponseEntity.ok(roomService.findAllRooms());
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomResponse> findRoom(@PathVariable  UUID roomId){
        return ResponseEntity.ok(roomService.getRoom(roomId));
    }

    @PostMapping()
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody RoomCreateRequest roomCreateRequest) {
        RoomResponse roomResponse = roomService.createRoom(roomCreateRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(roomResponse);
    }

    @PatchMapping("/{roomId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RoomResponse> updateRoom(@PathVariable UUID roomId, @Valid @RequestBody RoomUpdateRequest roomUpdateRequest) {
        RoomResponse roomResponse = roomService.updateRoom(roomId, roomUpdateRequest);
        return ResponseEntity.ok(roomResponse);
    }

    @DeleteMapping("/{roomId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRoom(@PathVariable UUID roomId) {
        roomService.deactivateRoom(roomId);
        return ResponseEntity.noContent().build();
    }
}
