package com.campus.campus_backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "errands")
public class Errand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "publisher_id", nullable = false)
    private User publisher;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "runner_id")
    private User runner;
    @Column(length = 255, nullable = false)
    private String content;
    @Column(length = 100)
    private String pickupAddr;
    @Column(length = 100)
    private String deliveryAddr;
    @Column(length = 255)
    private String hiddenInfo;
    @Column(precision = 10, scale = 2)
    private BigDecimal bounty;
    @Column(nullable = false, columnDefinition = "tinyint")
    private Short currency = 1;
    private LocalDateTime deadline;
    @Column(nullable = false, columnDefinition = "tinyint")
    private Short status = 0;
    @Version
    private Integer version;
    @CreationTimestamp
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
