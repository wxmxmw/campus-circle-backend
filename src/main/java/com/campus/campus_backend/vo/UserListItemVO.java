package com.campus.campus_backend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserListItemVO {
    private Long id;
    private String nickname;
    private String avatarUrl;
    private String bio;
    private Boolean isFollowing;
}