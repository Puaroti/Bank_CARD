package com.example.bankcards.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class UserEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 100)
    private String fullName;

    /** Фамилия пользователя. */
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    /** Имя пользователя. */
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    /** Отчество пользователя. */
    @Column(name = "patronymic", nullable = false, length = 100)
    private String patronymic;

    @Column(nullable = false, length = 20)
    private String role; // ADMIN or USER

    @OneToMany(mappedBy = "user")
    private Set<CardEntity> cards;
}
