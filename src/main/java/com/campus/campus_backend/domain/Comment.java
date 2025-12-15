package com.campus.campus_backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @Column
    private Long parentId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_to_user_id")
    private User replyToUser;
    @Column(length = 500, nullable = false)
    private String content;
    @Column
    private Integer likeCount = 0;
    @Column(nullable = false, columnDefinition = "tinyint")
    private Short status = 0;
    @CreationTimestamp
    private LocalDateTime createdAt;
}
