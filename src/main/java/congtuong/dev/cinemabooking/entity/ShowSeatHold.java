package congtuong.dev.cinemabooking.entity;

import congtuong.dev.cinemabooking.entity.enums.ShowSeatHoldStatus;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "show_seat_holds",
        indexes = {
                @Index(name = "idx_show_seat_hold_user", columnList = "user_id"),
                @Index(name = "idx_show_seat_hold_showtime", columnList = "showtime_id"),
                @Index(name = "idx_show_seat_hold_status", columnList = "status"),
                @Index(name = "idx_show_seat_hold_expires_at", columnList = "expires_at"),
                @Index(name = "idx_show_seat_hold_status_expires", columnList = "status,expires_at")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowSeatHold {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id", nullable = false)
    private ShowTime showtime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ShowSeatHoldStatus status;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

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
            status = ShowSeatHoldStatus.ACTIVE;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = new Timestamp(System.currentTimeMillis());
    }
}
