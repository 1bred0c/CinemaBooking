package congtuong.dev.cinemabooking.entity;

import congtuong.dev.cinemabooking.entity.enums.RoomStatus;
import congtuong.dev.cinemabooking.entity.enums.RoomType;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.UUID;

@Table(name = "rooms")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Room {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name ="name")
    private String name;

    @Column(name ="total_seats")
    private Integer totalSeats;

    @Column(name ="total_rows")
    private Integer totalRows;

    @Column(name ="total_columns")
    private Integer totalColumns;

    @Enumerated(EnumType.STRING)
    @Column(name ="room_type")
    private RoomType roomType;

    @Enumerated(EnumType.STRING)
    @Column(name ="status")
    private RoomStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id")
    private Cinema cinema;

    @Column(name ="created_at")
    private Timestamp createdAt;

    @Column(name ="updated_at")
    private Timestamp updatedAt;

    @PrePersist
    public void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        this.createdAt = now;
        this.updatedAt = now;
        status =  RoomStatus.ACTIVE;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

}
