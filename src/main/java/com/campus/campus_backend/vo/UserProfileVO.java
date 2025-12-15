package com.campus.campus_backend.vo;

import lombok.Data;

@Data
public class UserProfileVO {
    private Long id;
    private String username;
    private String nickname;
    private String avatarUrl;
    private String school;
    private Integer points;
    private String bio;
}
