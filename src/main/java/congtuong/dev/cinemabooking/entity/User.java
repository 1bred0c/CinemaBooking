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

    // Kept nullable for backward compatibility with users created before email was added.
    @Column(name = "email", unique = true)
    private String email;

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

    @Builder.Default
    @Column(name = "security_version", nullable = false, columnDefinition = "bigint default 1")
    private long securityVersion = 1L;

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
        if (this.securityVersion < 1L) {
            this.securityVersion = 1L;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updateAt = Timestamp.valueOf(LocalDateTime.now());
    }
}
