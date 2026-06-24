package com.example.shop.entity;

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
    @NotBlank(message = "Name can not be blank")
    @Size(max = 20, min = 5, message = "Name must be between 5 and 20 characters!")
    private String userName;
    @NotBlank(message = "Email can not be blank!")
    @Email(message = "Invalid email!")
    @Column(unique = true, nullable = false)
    private String email;
    @NotBlank(message = "Password can not be blank")
    private String password;
    private String phone;
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "userId"),
            inverseJoinColumns = @JoinColumn(name = "roleId")
    )
    private Set<Role> roles = new HashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_address",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "address_id")
    )
    private Set<Address> addresses;


    @ManyToMany(cascade = {CascadeType.MERGE,CascadeType.PERSIST},fetch = FetchType.EAGER)
   @JoinTable(
           name = "user_position",
           joinColumns = @JoinColumn(name = "userId"),
           inverseJoinColumns = @JoinColumn(name ="positionId")
   )
   private Set<Position>positions=new HashSet<>();

    @Past(message = "Data of birth is in wrong format")
    private LocalDate brithDay;
    private String updateBy;
    private LocalDateTime createAt;
    private LocalDateTime updateAt;

    @PrePersist
    private void onCreateAt() {
        this.createAt = LocalDateTime.now();
    }

    @PreUpdate
    private void onUpdateAt() {
        this.updateAt = LocalDateTime.now();
    }
}
