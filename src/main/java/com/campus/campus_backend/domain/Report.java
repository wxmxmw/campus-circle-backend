package com.campus.campus_backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "reports")
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long targetId;
    @Column(nullable = false, columnDefinition = "tinyint")
    private Short targetType;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;
    @Column(length = 50)
    private String reason;
    @Column(nullable = false, columnDefinition = "tinyint")
    private Short status = 0;
    @CreationTimestamp
    private LocalDateTime createdAt;
}
