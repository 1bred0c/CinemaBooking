package congtuong.dev.cinemabooking.entity;

import congtuong.dev.cinemabooking.entity.enums.Role;
import jakarta.persistence.*;
import lombok.*;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Table(name = "users")
@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "phone_number", nullable = false, unique = true)
    private String phoneNumber;

    @Column(name = "password", nullable = true)
    private String password;

    @Column(name = "fullname", nullable = false)
    private String fullname;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private Role role;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "create_at", nullable = false)
    private Timestamp createAt;

    @Column(name = "update_at", nullable = false)
    private Timestamp updateAt;

    @PrePersist
    public void prePersist() {
        Timestamp now = Timestamp.valueOf(LocalDateTime.now());
        this.createAt = now;
        this.updateAt = now;
        this.isActive = true;
    }

    @PreUpdate
    public void preUpdate() {
        this.updateAt = Timestamp.valueOf(LocalDateTime.now());
    }
}
