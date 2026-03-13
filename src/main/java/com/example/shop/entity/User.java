package com.example.shop.entity;

import com.example.shop.entity.enums.EAccountStatus;
import com.example.shop.entity.enums.EOnlineStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import jakarta.persistence.Table;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long userId;
    private String userName;
    @Column(unique = true, nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EAccountStatus accountStatus = EAccountStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EOnlineStatus onlineStatus = EOnlineStatus.OFFLINE;

    private String password;

    private String phone;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "userId"),
            inverseJoinColumns = @JoinColumn(name = "roleId")
    )
    private Set<Role> roles = new HashSet<>();

    @OneToOne(
            fetch = FetchType.LAZY,
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @JoinColumn(name = "avatar_id")
    private Image avatar;

    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY
    )

    private Set<Address> addresses = new HashSet<>();

    @ManyToMany(cascade = {CascadeType.MERGE, CascadeType.PERSIST}, fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_position",
            joinColumns = @JoinColumn(name = "userId"),
            inverseJoinColumns = @JoinColumn(name = "positionId")
    )
    private Set<Position> positions = new HashSet<>();
    private String provider;

    @Past(message = "Data of birth is in wrong format")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate brithDay;

    private String updateBy;

    private LocalDateTime createAt;

    private Long createBy;

    private LocalDateTime updatedAt;


    @PrePersist
    private void onCreateAt() {
        this.createAt = LocalDateTime.now();
    }

    @PreUpdate
    private void onUpdateAt() {
        this.updatedAt = LocalDateTime.now();
    }
}
