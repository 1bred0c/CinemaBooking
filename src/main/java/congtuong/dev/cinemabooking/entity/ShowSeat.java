package congtuong.dev.cinemabooking.entity;

import congtuong.dev.cinemabooking.entity.enums.ShowSeatStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(
        name = "show_seats",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_show_seat_showtime_seat",
                columnNames = {"showtime_id", "seat_id"}
        ),
        indexes = {
                @Index(name = "idx_show_seat_showtime", columnList = "showtime_id"),
                @Index(name = "idx_show_seat_status", columnList = "status"),
                @Index(name = "idx_show_seat_showtime_status", columnList = "showtime_id,status")
        }
)
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class ShowSeat {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id", nullable = false)
    private ShowTime showtime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShowSeatStatus status;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    @PrePersist
    void prePersist() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = ShowSeatStatus.AVAILABLE;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }
}
