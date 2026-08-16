package de.frauas.design.backend.user.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "role", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "password")
public abstract class BaseUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private String guid;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private boolean enabled = false;

    @Column(name = "registration_code")
    private String registrationCode;

    @Column(name = "registration_code_timeout")
    private LocalDateTime registrationCodeTimeout;

    @Column(name = "registration_used_moment")
    private LocalDateTime registrationUsedMoment;

    /** SEC-H3: consecutive failed password attempts since last successful login. */
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    /** SEC-H3: if non-null and in the future, the account is temporarily locked. */
    @Column(name = "locked_until")
    private Instant lockedUntil;

    public abstract String getRole();
}
