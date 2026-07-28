package congtuong.dev.cinemabooking.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.UUID;

@Entity
@Table(
        name = "show_seat_hold_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_hold_item_hold_show_seat",
                columnNames = {"show_seat_hold_id", "show_seat_id"}
        ),
        indexes = {
                @Index(name = "idx_hold_item_hold", columnList = "show_seat_hold_id"),
                @Index(name = "idx_hold_item_show_seat", columnList = "show_seat_id")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShowSeatHoldItem {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_seat_hold_id", nullable = false)
    private ShowSeatHold showSeatHold;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "show_seat_id", nullable = false)
    private ShowSeat showSeat;

    @Column(name = "held_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal heldPrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Timestamp createdAt;

    @PrePersist
    void prePersist() {
        createdAt = new Timestamp(System.currentTimeMillis());
    }
}
