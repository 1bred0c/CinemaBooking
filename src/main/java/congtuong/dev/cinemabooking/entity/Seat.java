package congtuong.dev.cinemabooking.entity;

import congtuong.dev.cinemabooking.entity.enums.SeatType;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.util.UUID;

@Table(name = "seats")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roomId", nullable = false)
    private Room room;

    private String row;
    private Integer number;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatType type;
    private boolean isActive;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    @PrePersist
    public void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        this.createdAt = now;
        this.updatedAt = now;
        this.isActive = true;
    }

    @PreUpdate
    public void preUpdate() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        this.updatedAt = now;
    }

}
