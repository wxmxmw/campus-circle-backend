package com.campus.campus_backend.vo;

import lombok.Data;

import java.util.List;

@Data
public class PostPublishRespVO {
    private Long id;
    private Long userId;
    private String userAvatar;
    private String userName;
    private String userLevel;
    private String time;
    private Long topicId;
    private String topicName;
    private String content;
    private List<String> images;
    private Integer views;
    private Integer comments;
    private Integer likes;
    private Integer shares;
}
