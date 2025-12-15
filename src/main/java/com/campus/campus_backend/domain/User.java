package com.campus.campus_backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true, length = 50)
    private String username;
    @Column(nullable = false, unique = true, length = 100)
    private String email;
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;
    @Column(length = 50)
    private String nickname;
    @Column(length = 255)
    private String avatarUrl;
    @Column(length = 20)
    private String studentId;
    @Column(length = 50)
    private String school;
    @Column(length = 200)
    private String bio;
    @Column(nullable = false)
    private Integer points = 0;
    @Column(nullable = false, length = 20)
    private String role;
    @Version
    private Integer version;
    @CreationTimestamp
    private LocalDateTime createdAt;
    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
