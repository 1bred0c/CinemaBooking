package congtuong.dev.cinemabooking.service;

import congtuong.dev.cinemabooking.dto.request.SeatLayoutCreateRequest;
import congtuong.dev.cinemabooking.dto.request.SeatRowCreateRequest;
import congtuong.dev.cinemabooking.dto.request.SeatSectionCreateRequest;
import congtuong.dev.cinemabooking.entity.Room;
import congtuong.dev.cinemabooking.entity.Seat;
import congtuong.dev.cinemabooking.entity.enums.RoomStatus;
import congtuong.dev.cinemabooking.entity.enums.SeatType;
import congtuong.dev.cinemabooking.exception.SeatException;
import congtuong.dev.cinemabooking.repository.RoomRepository;
import congtuong.dev.cinemabooking.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatServiceImplTest {

    @Mock
    private SeatRepository seatRepository;
    @Mock
    private RoomRepository roomRepository;

    private SeatServiceImpl seatService;
    private UUID roomId;
    private Room room;

    @BeforeEach
    void setUp() {
        seatService = new SeatServiceImpl(
                seatRepository,
                roomRepository
        );
        roomId = UUID.randomUUID();
        room = Room.builder()
                .id(roomId)
                .name("Room 1")
                .totalRows(2)
                .status(RoomStatus.ACTIVE)
                .build();
    }

    @Test
    void createSeatLayoutSupportsTypesAndGapsInOneRequest() {
        SeatLayoutCreateRequest request = new SeatLayoutCreateRequest(
                roomId,
                List.of(
                        new SeatRowCreateRequest(
                                " a ",
                                List.of(
                                        new SeatSectionCreateRequest(
                                                1,
                                                4,
                                                SeatType.STANDARD
                                        ),
                                        new SeatSectionCreateRequest(
                                                5,
                                                6,
                                                SeatType.PREMIUM
                                        ),
                                        new SeatSectionCreateRequest(
                                                8,
                                                8,
                                                SeatType.COUPLE
                                        )
                                )
                        )
                )
        );
        when(roomRepository.findByIdForUpdate(
                roomId,
                RoomStatus.ACTIVE
        )).thenReturn(Optional.of(room));
        when(seatRepository.findByRoomId(roomId)).thenReturn(List.of());
        when(seatRepository.saveAll(anyList())).thenAnswer(
                invocation -> invocation.getArgument(0)
        );

        var response = seatService.createSeatLayout(request);

        assertThat(response).hasSize(7);
        assertThat(response)
                .extracting(seat -> seat.row() + seat.number())
                .containsExactly("A1", "A2", "A3", "A4", "A5", "A6", "A8");
        assertThat(response)
                .extracting(seat -> seat.type())
                .containsExactly(
                        SeatType.STANDARD,
                        SeatType.STANDARD,
                        SeatType.STANDARD,
                        SeatType.STANDARD,
                        SeatType.PREMIUM,
                        SeatType.PREMIUM,
                        SeatType.COUPLE
                );
    }

    @Test
    void createSeatLayoutRejectsOverlappingSectionsAtomically() {
        SeatLayoutCreateRequest request = new SeatLayoutCreateRequest(
                roomId,
                List.of(
                        new SeatRowCreateRequest(
                                "A",
                                List.of(
                                        new SeatSectionCreateRequest(
                                                1,
                                                5,
                                                SeatType.STANDARD
                                        ),
                                        new SeatSectionCreateRequest(
                                                5,
                                                8,
                                                SeatType.PREMIUM
                                        )
                                )
                        )
                )
        );
        when(roomRepository.findByIdForUpdate(
                roomId,
                RoomStatus.ACTIVE
        )).thenReturn(Optional.of(room));
        when(seatRepository.findByRoomId(roomId)).thenReturn(List.of());

        assertThatThrownBy(() -> seatService.createSeatLayout(request))
                .isInstanceOf(SeatException.class)
                .hasMessageContaining("A5")
                .hasMessageContaining("duplicated");
        verify(seatRepository, never()).saveAll(anyList());
    }
}
