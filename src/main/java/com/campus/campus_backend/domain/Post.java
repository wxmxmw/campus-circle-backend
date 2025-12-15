package com.campus.campus_backend.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "posts")
public class Post {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    @Column(nullable = false)
    private Boolean isAnonymous = false;
    @Column(nullable = false, columnDefinition = "text")
    private String content;
    @Column(length = 500)
    private String summary;
    @Column(columnDefinition = "json")
    private String images;
    @Column(length = 255)
    private String tags;
    @Column(columnDefinition = "json")
    private String productInfo;
    @Column
    private Integer viewCount = 0;
    @Column
    private Integer likeCount = 0;
    @Column
    private Integer commentCount = 0;
    @Column
    private Integer collectCount = 0;
    @Column
    private Integer hotScore = 0;
    @Column
    private Boolean isSticky = false;
    @Column(nullable = false, columnDefinition = "tinyint")
    private Short status = 0;
    @CreationTimestamp
    private LocalDateTime createdAt;
}
