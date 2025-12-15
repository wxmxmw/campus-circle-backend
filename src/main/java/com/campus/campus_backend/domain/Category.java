package com.campus.campus_backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 50)
    private String name;
    @Column(length = 255)
    private String icon;
    @Column(nullable = false)
    private Integer sortOrder = 0;
    @Column(nullable = false)
    private Boolean isActive = true;
    @CreationTimestamp
    private LocalDateTime createdAt;
}

