package congtuong.dev.cinemabooking.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "cinemas")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Cinema {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "name",  nullable = false)
    @NotBlank
    private String name;

    @Column(name = "address",  nullable = false)
    @NotBlank
    private String address;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "created_at", nullable = false)
    private Timestamp createdAt;

    @Column(name = "updated_at", nullable = false)
    private Timestamp updatedAt;

    @PrePersist
    public void prePersist() {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        this.createdAt = now;
        this.updatedAt = now;
        this.isActive = true;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Timestamp.valueOf(LocalDateTime.now());
    }
}
