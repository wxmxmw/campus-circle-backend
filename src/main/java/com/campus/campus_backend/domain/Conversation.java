package com.campus.campus_backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "conversations", uniqueConstraints = @UniqueConstraint(columnNames = {"user1_id","user2_id"}))
public class Conversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user1_id", nullable = false)
    private User user1;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user2_id", nullable = false)
    private User user2;
    @Column
    private Long lastMessageId;
    private LocalDateTime lastMessageTime;
    @Column(nullable = false)
    private Integer user1Unread = 0;
    @Column(nullable = false)
    private Integer user2Unread = 0;
    @CreationTimestamp
    private LocalDateTime createdAt;
}

